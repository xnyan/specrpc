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
import java.net.UnknownHostException;
import java.util.concurrent.ExecutionException;

import Waterloo.MultiSocket.exception.ConnectionCloseException;
import Waterloo.MultiSocket.exception.MultiSocketValidException;

import rpc.communication.Communication;
import rpc.communication.Message;
import specrpc.common.Location;

/*
 * RPCCommThread starts the RPC request, waits for RPC response,
 * and provides the function of sending more messages (if any).
 */
public class RpcCommunication implements Runnable {
  // RPC communication Model
  private final Communication commModule;
  private final ControlThread controlThread;

  public RpcCommunication(Location serverLocation, ControlThread controlThread) throws UnknownHostException,
      IOException, InterruptedException, ExecutionException, MultiSocketValidException, ConnectionCloseException {
    this.commModule = Communication.connectTo(serverLocation);
    this.controlThread = controlThread;
  }

  public void send(Message msg)
      throws InterruptedException, IOException, MultiSocketValidException, ConnectionCloseException {
    // Server provides a thread-safe send, so we don't need to make this
    // synchronized
    commModule.send(msg.serialize());
  }

  // ControlThread is responsible for closing the communication module
  public void closeCommModule() {
    try {
      commModule.disconnect();
    } catch (IOException e) {
      e.printStackTrace();
    } catch (MultiSocketValidException e) {
      e.printStackTrace();
    } catch (ConnectionCloseException e) {
      e.printStackTrace();
    }
  }

  public synchronized void run() {
    try {
      while (this.controlThread.isReadNextMessage()) {// block until ControlThread allow this to read
        // Block on the communication module until:
        // (1) read message from communication module;
        // (2) controlThread closes communication module;
        // (3) server side close communication module;
        //
        // In (2) & (3), there will be two situations:
        // (a) getMessage() return null, because getMessage() is
        // waken up soon after the communication close;
        // (b) SocketException is thrown, because getMessage() is
        // waken up long after the communication close,
        // in which case the timeout of the socket expires.
        // Two kinds of SocketException would happen:
        // (i) socket closed; and (ii) socket connection reset.

        String responseMsg = commModule.getMessage(); // null or SockeException

        if (responseMsg != null) {
          // Normal message
          // Blocks on controlThread's monitor until be waken up
          controlThread.deliverMessage(responseMsg);
        } else {
          // Null is read when communication module is closed or exception happens
          // TODO: Distinguishes these two scenarios.
          break;
        }
      }
    } catch (InterruptedException | ConnectionCloseException e) {
      this.handleException(e);
    }
  }

  private synchronized void handleException(Exception e) {
    // Exception happens because of ControlThread closes socket or buffer
    if (this.controlThread.isTerminated()) {
      // Does nothing
      ;
    } else {
      // Lets the ControlThread know exception happens and not wait on message
      // TODO: we need a better way to deal with different types of exceptions
      e.printStackTrace(); // Unknown exception happens
      try {
        // The exception is passed to controlThread as RpcUserException
        this.controlThread.deliverException(e);
      } catch (InterruptedException e1) {
        e1.printStackTrace();
      }
    }
  }
}
