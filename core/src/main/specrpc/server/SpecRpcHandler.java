/* 
 * Copyright 2017 SpecRPC authors                                                                           
 *                                                                                                                       
 * Licensed under the Apache License, Version 2.0 (the "License");                                                      
 * you may not use this file except in compliance with the License.                                                     
 * You may obtain a copy of the License at                                                                              
 *                                                                                                                      
 *     http://www.apache.org/licenses/LICENSE-2.0                                                                       
 *                                                                                                                      
 * Unless required by applicable law or agreed to in writing, software                                                  
 * distributed under the License is distributed on an "AS IS" BASIS,                                                    
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.                                             
 * See the License for the specific language governing permissions and                                                  
 * limitations under the License. 
 */

package specrpc.server;

import java.io.IOException;
import java.util.concurrent.ExecutorService;

import Waterloo.MultiSocket.IConnection;
import Waterloo.MultiSocket.exception.ConnectionCloseException;
import Waterloo.MultiSocket.exception.MultiSocketValidException;

import com.google.gson.JsonSyntaxException;

import rpc.communication.Communication;
import rpc.execption.MethodNotRegisteredException;
import rpc.execption.NoClientStubException;
import specrpc.common.SpecRpcFacadeObject;
import specrpc.common.Status;
import specrpc.common.Status.SpeculationStatus;
import specrpc.communication.CallerSpeculationSolvedMsg;
import specrpc.communication.RequestMsg;
import specrpc.communication.CallerSpeculationSolvedMsg.Resolution;
import specrpc.exception.SpeculationFailException;
import specrpc.server.api.SpecRpcHost;

public class SpecRpcHandler implements Runnable {

  private final IConnection clientConnection;
  // Mapping between RPC signature and its host object
  private final SpecRpcHostObjectMap rpcHostObjectDictionary;
  private final ExecutorService threadPool;
  private SpecRpcFacadeObject specRpcFacade;

  public SpecRpcHandler(IConnection connection, SpecRpcHostObjectMap localdir, ExecutorService threadPool) {
    this.clientConnection = connection;
    this.rpcHostObjectDictionary = localdir;
    this.threadPool = threadPool;
    this.specRpcFacade = null;
  }

  @Override
  public void run() {
    Communication commModule = null;
    SpecRpcServerStub clientStub = null;
    try {
      // Initializes communication module
      commModule = new Communication(clientConnection);
      clientStub = new SpecRpcHandlerClientStub(commModule);

      // Reads an RPC request message
      String request = commModule.getMessage();
      // Parses the RPC request message
      // If the message is invalid, catch the exception and send an exception message
      // to the client.
      RequestMsg requestMsg = new RequestMsg(request);

      // Looks up the host object of the RPC method
      SpecRpcHost hostObject = this.rpcHostObjectDictionary.getHostObject(requestMsg.signature);

      // RPC caller's speculation status
      // The speculation status of RPC host object only depends on the RPC caller's
      // speculation status.
      // There is no callee for an RPC as it does not wait for any RPC response
      // (whereas Callback has a callee).
      // Sets callee's speculation status as SUCCEED since we use the same abstraction
      // as Callback uses.
      Status rpcCallerStatus = new Status(requestMsg.callerStatus, SpeculationStatus.SUCCEED);

      // Binds ISpecRPCFacade that controls speculation status and will send RPC
      // response back to client
      this.specRpcFacade = new SpecRpcFacadeObject(clientStub, rpcCallerStatus, this.threadPool);
      hostObject.bind(this.specRpcFacade);

      // Executes RPC method
      SpecRpcExecutor rpcExecutor = new SpecRpcExecutor(this.specRpcFacade, hostObject, requestMsg.signature,
          requestMsg.args);
      this.threadPool.execute(rpcExecutor);

      // If RPC caller is speculative, waits for its notification about its
      // speculation status change.
      // This notification may not arrive because of unexpected exceptions.
      this.checkIfCallerSpecSolved(commModule);

      // Blocks until the RPC method instance finishes running
      rpcExecutor.join();

      /*
       * When speculation fails, executes a rollback function that is registered by
       * applications.
       */
      if (this.specRpcFacade.getCallerStatus() == SpeculationStatus.FAIL) {
        if (this.specRpcFacade.isRollbackRegistered()) {
          this.specRpcFacade.executeRollback();
        }
      }

      // Before closing connection, makes sure that server returns the final response
      // that may be returned later by a callback after the RPC method finishes.
      // This only applies to the RPC caller that has SUCCEED speculation status
      // Up to this stage, Caller's speculation status must be FAIL or SUCCEED
      if (this.specRpcFacade.getCallerStatus() == SpeculationStatus.SUCCEED) {
        // Wait until the actual result is sent back to client
        ((SpecRpcHandlerClientStub) clientStub).waitUnitlSendActualReturn();
      }
      // else if (this.specRpcFacade.getCallerStatus() ==
      // SpeculationStatus.SPECULATIVE) {
      // logger.error("Caller's speculation status must be FAIL or SUCCEED. Should not
      // execute up to here. Check the error!");
      // }

      /*
       * Alternative Solution: Waits for client to close the connection This solution
       * does not provide a good performance
       */
      // String unexpectedMsg = null;
      // do {
      // try {
      // unexpectedMsg = commModule.getMessage();
      // } catch (IOException e) {
      // if(e instanceof java.net.SocketException) {
      // unexpectedMsg = null;
      // }
      // }
      // } while (unexpectedMsg != null);

      // Everything is done now, close the communication in the "finally"

    } catch (IOException | JsonSyntaxException | ClassNotFoundException | SecurityException | IllegalArgumentException
        | InterruptedException | MethodNotRegisteredException | NullPointerException | ConnectionCloseException e) {
      handleException(clientStub, e);
    } finally {
      try {
        // Everything is done, or exception happens, so close the communication
        commModule.disconnect();
      } catch (IOException | MultiSocketValidException | ConnectionCloseException e) {
        e.printStackTrace();
      }
    }
  }

  private void handleException(SpecRpcServerStub clientStub, Exception e) {
    try {
      // Notifies the RPCs (if any) invoked from this RPC method
      // TODO: do we really need this?
      if (this.specRpcFacade != null) {
        // TODO: Uses a better way to deal with exceptions
        this.specRpcFacade.setCallerStatus(SpeculationStatus.FAIL);
      }
      // Notifies the client that an exception happened
      clientStub.throwNonSpecException(e.toString());
    } catch (NoClientStubException | SpeculationFailException | InterruptedException | IOException
        | MultiSocketValidException | ConnectionCloseException e1) {
      e1.printStackTrace();
    }
    e.printStackTrace();
  }

  /*
   * If an RPC caller is speculative, waits for the caller's speculation status to
   * become SUCCEED or FAIL
   */
  private void checkIfCallerSpecSolved(Communication commModule) throws IOException, InterruptedException {

    if (this.specRpcFacade.getCurrentRpcStatus() != SpeculationStatus.SPECULATIVE) {
      return;
    }

    // Reads message
    // The client may have already closed the communication. In this case, with
    // using socket, null will be returned or exception will be thrown.
    String jsonMessage = null;
    try {
      // If the client closed the communication, we will get either null or an
      // exception.
      jsonMessage = commModule.getMessage();

      if (jsonMessage == null) {
        // Notifies the RPC instance and any subsequent RPCs that is invoked through the
        // RPC instance.
        this.specRpcFacade.setCallerStatus(SpeculationStatus.FAIL);
        return;
      }
    } catch (ConnectionCloseException e) {
      // When using the Multisocket package
      // Notifies the RPC instance and any subsequent RPCs that is invoked through the
      // RPC instance.
      this.specRpcFacade.setCallerStatus(SpeculationStatus.FAIL);
      return;
    }
    // catch (IOException e) {
    // // When using the Java Socket
    // // The exception should be SocketException.
    // if (e instanceof java.net.SocketException) {
    // // Notifies the RPC instance and any subsequent RPCs that is invoked through
    // the RPC instance.
    // this.specRpcFacade.setCallerStatus(SpeculationStatus.FAIL);
    // return;
    // }
    // }

    // Parses the message
    CallerSpeculationSolvedMsg msg = new CallerSpeculationSolvedMsg(jsonMessage);
    // Notifies the RPC method and its invoking RPCs (that is invoked in the RPC
    // method)
    if (msg.resolType == Resolution.COMMIT) {
      // the PRC caller's speculation status is SUCCEED
      this.specRpcFacade.setCallerStatus(SpeculationStatus.SUCCEED);
    } else {
      // the RPC caller's speculation status is FAIL
      this.specRpcFacade.setCallerStatus(SpeculationStatus.FAIL);
    }
  }
}
