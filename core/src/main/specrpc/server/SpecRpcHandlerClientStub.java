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

import Waterloo.MultiSocket.exception.ConnectionCloseException;
import Waterloo.MultiSocket.exception.MultiSocketValidException;
import rpc.communication.Communication;
import specrpc.common.Status.SpeculationStatus;
import specrpc.communication.ResponseExceptionMsg;
import specrpc.communication.ResponseValueMsg;
import specrpc.communication.SpeculativeResponseValueMsg;

public class SpecRpcHandlerClientStub implements SpecRpcServerStub {

  private final Communication communicationChannel;
  private boolean isMsgActualReturned; // Will be updated only once, which then closes communication channel

  public SpecRpcHandlerClientStub(Communication comChannel) {
    this.communicationChannel = comChannel;
    this.isMsgActualReturned = false;
  }

  @Override
  public synchronized void sendNonSpecReturn(Object value)
      throws InterruptedException, IOException, MultiSocketValidException, ConnectionCloseException {
    String msg = new ResponseValueMsg(value).serialize();
    this.communicationChannel.send(msg);
    this.isMsgActualReturned = true;
    notifyAll();
  }

  @Override
  public synchronized void sendSpecReturn(Object value)
      throws InterruptedException, IOException, MultiSocketValidException, ConnectionCloseException {
    String msg = new SpeculativeResponseValueMsg(value).serialize();
    this.communicationChannel.send(msg);
  }

  @Override
  public synchronized void throwNonSpecException(String message)
      throws InterruptedException, IOException, MultiSocketValidException, ConnectionCloseException {
    String msg = new ResponseExceptionMsg(message).serialize();
    this.communicationChannel.send(msg);
    this.isMsgActualReturned = true;
    notifyAll();
  }

  @Override
  public synchronized void callbackStatusChanged(SpeculationStatus callbackStatus) {
    // Does nothing
  }

  public synchronized void waitUnitlSendActualReturn() throws InterruptedException {
    while (this.isMsgActualReturned == false) {
      wait();
    }
  }
}
