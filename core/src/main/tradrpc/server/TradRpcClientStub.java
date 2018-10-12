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

import Waterloo.MultiSocket.exception.ConnectionCloseException;
import Waterloo.MultiSocket.exception.MultiSocketValidException;

import rpc.communication.Communication;
import tradrpc.communication.TradRpcResponseMsg;

public class TradRpcClientStub {

  private final Communication comChannel;
  private boolean isSentException = false;

  public TradRpcClientStub(Communication comChannel) {
    this.comChannel = comChannel;
  }

  protected synchronized void send(Object value)
      throws InterruptedException, IOException, MultiSocketValidException, ConnectionCloseException {
    String msg = new TradRpcResponseMsg(TradRpcResponseMsg.MessageType.RETURN, value).serialize();
    this.comChannel.send(msg);
  }

  public synchronized void sendException(String exceptionMsg)
      throws InterruptedException, IOException, MultiSocketValidException, ConnectionCloseException {
    this.isSentException = true;
    String msg = new TradRpcResponseMsg(TradRpcResponseMsg.MessageType.EXCEPTION, exceptionMsg).serialize();
    this.comChannel.send(msg);
  }

  protected synchronized boolean isSentException() {
    return isSentException;
  }
}
