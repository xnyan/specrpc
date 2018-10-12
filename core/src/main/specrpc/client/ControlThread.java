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

package specrpc.client;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;

import Waterloo.MultiSocket.exception.ConnectionCloseException;
import Waterloo.MultiSocket.exception.MultiSocketValidException;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;

import rpc.execption.UserException;
import specrpc.client.api.SpecRpcStatistics;
import specrpc.common.StatusListener;
import specrpc.common.Location;
import specrpc.common.RpcSignature;
import specrpc.common.Status.SpeculationStatus;
import specrpc.communication.CallerSpeculationSolvedMsg;
import specrpc.communication.RequestMsg;
import specrpc.communication.ResponseExceptionMsg;
import specrpc.communication.ResponseMsg;
import specrpc.communication.ResponseValueMsg;
import specrpc.communication.SpeculativeResponseValueMsg;
import specrpc.communication.CallerSpeculationSolvedMsg.Resolution;
import specrpc.exception.UnexpectedResponseTypeException;

public class ControlThread implements Runnable, StatusListener {

  private final ExecutorService clientThreadPool;
  private final RpcSignature signature;
  private final Object[] args;
  private final Location serverLocation;
  private final CallbackManager callbacksManager;
  private final SpecRpcFutureObject future;
  private SpeculationStatus callerStatus;

  // Flag for whether the status of caller changed
  private boolean callerStatusChanged;

  // Flag for whether controlThread to listen on status change or more RPC
  // response
  private boolean controlThreadTerminate;

  // Flag for controlling RPCCommunicationThread deliver message
  private boolean enableReadNextMessage;

  // Flag for controlling reading RPC response message
  private boolean nextMessageReady;
  private String nextMessage;

  public ControlThread(RpcSignature signature, Object[] args, Location serverLocation,
      CallbackManager callbacksManager, SpecRpcFutureObject future, SpeculationStatus callerStatus,
      ExecutorService threadPool) {
    this.signature = signature;
    this.args = args;
    this.serverLocation = serverLocation;
    this.callbacksManager = callbacksManager;
    this.future = future;
    this.callerStatus = callerStatus;
    this.clientThreadPool = threadPool;

    // Initializes flags
    this.callerStatusChanged = false;
    this.controlThreadTerminate = false;
    this.enableReadNextMessage = false;
    this.nextMessageReady = false;
    this.nextMessage = null;
  }

  @Override
  public synchronized void run() {
    /*
     * Immediately aborts if the previous caller's status is FAILED. This is just a
     * quick check so we don't waste a lot of time if we immediately know that the
     * previous speculative was false. Save the value of the caller status and use
     * that value until we serialize and send the RPC request.
     */
    if (this.callerStatus == SpeculationStatus.FAIL) {
      // notify callbacks to abort
      this.callbacksManager.callerStatusChanged(this.callerStatus);
      // notify future to the callerStatus changed
      this.future.callerStatusChanged(this.callerStatus);
      return;
    }

    RpcCommunication rpcCommThread = null;
    try {
      // The actual thread of rpcCommThread is responsible for receiving messages
      // The actual thread of ControlThread (this) is responsible for sending
      // messages via calling the methods of rpcCommThread
      // Communication connection is made in RPCCommunicationThread constructor
      rpcCommThread = new RpcCommunication(this.serverLocation, this);
      this.clientThreadPool.execute(rpcCommThread);// start the thread running rpcCommThread
      // Invokes RPC, i.e., sending RPC request message.
      rpcCommThread.send(new RequestMsg(this.callerStatus, this.signature, this.args));

      // Enables rpcCommThread to read message
      this.enableReadNextMessage = true;

      // Parses RPC response messages
      Gson gson = new Gson();
      JsonParser parser = new JsonParser();

      while (!controlThreadTerminate) {
        // Handles speculation status change, only for SUCCEED & FAIL
        if (callerStatusChanged) {
          callerStatusChanged = false;

          // CallbacksManager & Future do not automatically listen on the callerStatus.
          // We need to notify them that the callerStatus changed.
          this.callbacksManager.callerStatusChanged(this.callerStatus);
          this.future.callerStatusChanged(this.callerStatus);

          // Notifies RPC servers that speculation status changed and
          // decides if we need to wait for more RPC response messages.
          switch (this.callerStatus) {
          case FAIL: {
            rpcCommThread.send(new CallerSpeculationSolvedMsg(Resolution.ABORT));
            controlThreadTerminate = true;
            break;
          }
          case SUCCEED: {
            rpcCommThread.send(new CallerSpeculationSolvedMsg(Resolution.COMMIT));
            // Checks whether callee (i.e., RPC server) also commits.
            if (this.callbacksManager.isCalleeSpecSolved()) {
              // Delivers the result of correctCallBack to future.
              // This may block until the correctCallbackRunner finishes running.
              future.deliver(this.callbacksManager.getCorrectCallbackRunner().getResult());
              controlThreadTerminate = true;
            }
            break;
          }
          default: {
            // This should not happen
            break;
          }
          }
        } else if (nextMessageReady) {
          // Handle a new RPC response message
          // Consumes the message flag
          nextMessageReady = false;
          // Parses the received message
          JsonArray array = parser.parse(nextMessage).getAsJsonArray();
          ResponseMsg.ResponseType type = gson.fromJson(array.get(0), ResponseMsg.ResponseType.class);
          switch (type) {
          case RETURN: {
            ResponseValueMsg valueMsg = new ResponseValueMsg(gson.fromJson(array.get(1), this.signature.returnType));
            // When deciding the correct CallbackRunner,
            // CallbacksManager will invalidate other CallbackRunners.
            CallbackRunner correctCallbackRunner = this.callbacksManager
                .determineCorrectCallbackRunner(valueMsg.returnValue);
            // Checks whether caller also commits
            if (correctCallbackRunner.getCallbackStatus() == SpeculationStatus.SUCCEED) {
              this.future.deliver(correctCallbackRunner.getResult());
              controlThreadTerminate = true;
            }
            break;
          }
          case SPEC_RETURN: {
            SpeculativeResponseValueMsg specMsg = new SpeculativeResponseValueMsg(
                gson.fromJson(array.get(1), this.signature.returnType));
            // If there is no such a CallbackRunner running with this speculative value,
            // starts such a CallbackRunner.
            if (null == this.callbacksManager.getCallbackRunner(specMsg.returnValue)) {
              this.callbacksManager.runCallback(specMsg.returnValue, SpeculationStatus.SPECULATIVE);
              if (SpecRpcStatistics.isEnabled) {
                SpecRpcStatistics.increaseTotalPredictionNumber();
              }
            }
            break;
          }
          case EXCEPTION: {
            ResponseExceptionMsg exceptionMsg = new ResponseExceptionMsg(gson.fromJson(array.get(1), String.class));
            // Delivers the exception to future
            this.future.deliverException(new UserException(exceptionMsg.getExceptionMsg()));

            // Notifies all callbacks that the RPC failed
            this.callbacksManager.invalidateAllCallbackRunners();
            controlThreadTerminate = true;
            break;
          }
          default: {
            // Should not happen here
            // TODO : needs a better fault handler, notifies future and callbacks, and
            // terminates RPCCommThread
            throw new UnexpectedResponseTypeException(type);
          }
          }

          // If not terminated, allows RPCCommThread to read the next message.
          if (!controlThreadTerminate) {
            this.enableReadNextMessage = true;
          }
        } else {
          // ControlThread waits for status change or more messages.
          // Notifies RPCCommThread to deliver message if any.
          notifyAll();
          // Waits for status change or new message.
          wait();
        }

        // ControlThread is terminating
        if (this.controlThreadTerminate == true) {
          // Notifies RPCCommThread to run if it blocks on ControlThread's
          // monitor.
          notifyAll();
        }
      }
    } catch (IOException | InterruptedException | UnexpectedResponseTypeException | MultiSocketValidException
        | ConnectionCloseException | ExecutionException e) {
      this.handleException(e);
    } finally {
      // Done. ContolThread terminates, closes the communication module on which the
      // RPCCommThread blocks
      if (rpcCommThread != null) {
        rpcCommThread.closeCommModule();
      }
    }
  }

  private synchronized void handleException(Exception e) {
    // Notifies callbacks that an exception happens.
    this.callbacksManager.invalidateAllCallbackRunners();
    // Notifies future that exception happens
    this.future.deliverException(new UserException(e.getMessage()));
  }

  // For RPCCommThread to check whether it is allowed to read message
  public synchronized boolean isReadNextMessage() throws InterruptedException {
    while (this.controlThreadTerminate == false && this.enableReadNextMessage == false) {
      wait();
    }

    if (this.controlThreadTerminate == true) {
      return false;
    }

    return this.enableReadNextMessage;// must be true
  }

  // Checks whether ControlThread terminates
  public synchronized boolean isTerminated() {
    return this.controlThreadTerminate;
  }

  // For RPCCommunicationThread to deliver message and wake up the ControlThread
  public synchronized void deliverMessage(String message) throws InterruptedException {
    nextMessageReady = true;
    nextMessage = message;
    this.enableReadNextMessage = false;
    notifyAll();
  }

  // For RPCCommunicationThread to deliver unexpected exceptions and wakes up the
  // ControlThread
  // TODO: identify unexpected exceptions
  public synchronized void deliverException(Exception e) throws InterruptedException {
    nextMessageReady = true;
    nextMessage = new ResponseExceptionMsg(e.toString()).serialize();
    this.enableReadNextMessage = false;
    notifyAll();
  }

  // Listens on callerStatus
  // When callerStatus changes, this method will be triggered
  // and wakes up the blocking thread.
  @Override
  public synchronized void statusChanged(SpeculationStatus status) {
    if (status != SpeculationStatus.SPECULATIVE && this.callerStatus != status) {
      this.callerStatus = status;
      // Notifies that caller status changed
      callerStatusChanged = true;
      // Wakes up blocking thread
      notifyAll();
    }
  }
}
