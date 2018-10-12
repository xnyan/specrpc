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

import rpc.execption.UserException;
import specrpc.client.api.SpecRpcFuture;
import specrpc.common.Status.SpeculationStatus;
import specrpc.exception.SpeculationFailException;

/*
 * A future can complete either by having the (actual) callback return a
 * value (deliver), throw an exception (deliverException), or if the client
 * RPC that it is dependent on (in an iterative speculation) change its 
 * status to FAIL.
 */

// Future listens on caller's Status not the status of current callback
// this means that if server sends back the actual result, current callback
// will return result.
public class SpecRpcFutureObject implements SpecRpcFuture {

  private Object result = null;
  private Boolean resultDelivered; // The actual result could be null
  private UserException exception = null;
  private SpeculationStatus callerStatus;// This is not the status of current callbacks

  public SpecRpcFutureObject(SpeculationStatus callerStatus) {
    this.resultDelivered = false;
    this.callerStatus = callerStatus;
  }

  @Override
  public synchronized Object getResult() throws InterruptedException, UserException, SpeculationFailException {
    while (this.resultDelivered == false && this.exception == null && this.callerStatus != SpeculationStatus.FAIL) {
      // Waits until there is a status change, or we can a result or exception.
      this.wait();
    }

    // TODO: Need to check callback's status?
    if (this.callerStatus == SpeculationStatus.FAIL) {
      throw new SpeculationFailException();
    }

    if (this.exception != null) {
      throw this.exception;
    }

    // Returns the actual result, which could be null.
    return result;
  }

  @Override
  public synchronized void deliver(Object result) {
    this.result = result;
    this.resultDelivered = true;
    this.notifyAll();
  }

  @Override
  public synchronized void deliverException(UserException exception) {
    this.exception = exception;
    this.notifyAll();
  }

  // ControlThread is responsible for notifying Future that callerStatus changes
  public synchronized void callerStatusChanged(SpeculationStatus callerStatus) {
    if (this.callerStatus != callerStatus) {
      this.callerStatus = callerStatus;
      this.notifyAll();
    }
  }
}
