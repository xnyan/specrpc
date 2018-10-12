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

package tradrpc.server;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import Waterloo.MultiSocket.IConnection;
import Waterloo.MultiSocket.exception.ConnectionCloseException;
import Waterloo.MultiSocket.exception.MultiSocketValidException;

import com.google.gson.JsonSyntaxException;

import rpc.communication.Communication;
import rpc.execption.MethodNotRegisteredException;
import specrpc.common.RpcSignature;
import tradrpc.communication.TradRpcRequestMsg;
import tradrpc.server.api.TradRpcHost;

public class TradRpcHandler implements Runnable {

  // private final Socket clientSocket;
  private final IConnection clientConnection;
  private final TradRpcHostObjectMap localDir;

  private RpcSignature signature;
  private Object[] args;
  private TradRpcHost hostObject;

  public TradRpcHandler(IConnection connection, TradRpcHostObjectMap localdir) {
    // this.clientSocket = socket;
    this.clientConnection = connection;
    this.localDir = localdir;
  }

  @Override
  public void run() {
    Communication com = null;
    try {
      // com = new Communication(clientSocket);
      com = new Communication(this.clientConnection);

      String request = com.getMessage();

      TradRpcRequestMsg requestMsg = new TradRpcRequestMsg(request);
      this.signature = requestMsg.signature;
      this.args = requestMsg.args;
      this.hostObject = this.localDir.getHostObject(signature);
      TradRpcClientStub clientStub = new TradRpcClientStub(com);
      this.hostObject.bind(clientStub);

      Class<? extends TradRpcHost> hostObjClass = this.hostObject.getClass();
      Method method = hostObjClass.getMethod(this.signature.methodName, this.signature.argTypes);
      Object result = method.invoke(this.hostObject, this.args);
      try {
        if (!clientStub.isSentException()) {
          clientStub.send(result);
        }
      } catch (IOException e) {
        e.printStackTrace();
      } catch (MultiSocketValidException e) {
        e.printStackTrace();
      }

      // socket connection is closed in 'finally'

      // // Begin for testing
      // // If the server is waiting for the client to firstly close socket will
      // // cause performance problem
      // String unexpectedMsg = null;
      // do {
      // try {
      // unexpectedMsg = com.getMessage();
      // } catch (IOException e) {
      // if (e instanceof java.net.SocketException) {
      // unexpectedMsg = null;
      // }
      // }
      // } while (unexpectedMsg != null);
      // // End for testing

    } catch (JsonSyntaxException | ClassNotFoundException | MethodNotRegisteredException | SecurityException
        | IllegalArgumentException | InterruptedException | IllegalAccessException | InvocationTargetException
        | NoSuchMethodException | ConnectionCloseException e) {
      e.printStackTrace();
    } finally {
      if (com != null) {
        try {
          // Closes the socket for avoiding socket exception: too many files
          com.disconnect();
        } catch (IOException | MultiSocketValidException | ConnectionCloseException e) {
          e.printStackTrace();
        }
      }
    }

  }
}
