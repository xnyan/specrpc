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

import specrpc.client.api.SpecRpcCallback;
import specrpc.common.SpecRpcFacadeObject;
import specrpc.common.Status;
import specrpc.common.Status.SpeculationStatus;
import specrpc.exception.SpeculationFailException;

public class CallbackRunner implements Runnable {

  private SpecRpcCallback callback;
  private SpecRpcFacadeObject specRpcFacade;
  private Object rpcReturnValue;
  private Object callbackResult;
  private boolean isCallbackFinished;// flag indicating that whether this callback finished running

  public CallbackRunner(SpecRpcCallback callback, Status callbackStatus, SpecRpcFacadeObject specRpcFacade,
      Object rpcReturnValue) {
    this.callback = callback;
    this.specRpcFacade = specRpcFacade;
    this.callback.bind(this.specRpcFacade);
    this.rpcReturnValue = rpcReturnValue;
    this.callbackResult = null;
    this.isCallbackFinished = false;
  }

  public synchronized void run() {
    try {
      this.callbackResult = this.callback.run(this.rpcReturnValue);
      this.isCallbackFinished = true;
      notifyAll();
    } catch (InterruptedException e) {
      e.printStackTrace();
    } catch (SpeculationFailException e) {
      ; // Callback speculation failed, so just aborts execution if there is no registered rollback function.
    } finally {
      // Performs any registered rollback function.
      // SpecRPC currently assumes that applications rollback after allowing any speculative execution completes.
      if (this.specRpcFacade.isRollbackRegistered()) {
        try {
          // Waits until speculative states have been successful or failed.
          this.specRpcFacade.specBlock();
        } catch (InterruptedException e) {
          e.printStackTrace();
        } catch (SpeculationFailException e) {
          // Rollbacks
          this.specRpcFacade.executeRollback();
        }
      }
    }
  }

  public synchronized Object getResult() throws InterruptedException {
    while (isCallbackFinished == false) {
      wait();
    }
    return this.callbackResult;
  }

  // setCallerStatus() & setCalleeStatus() should not be synchronized, because the
  // thread running CallbackRunner owns the monitor until finishes running
  // client-specified Callback that may block on SpecRPCFacade.specBlock()
  // or waiting for next-level callback returns result (future.getResult())
  public void setCallerStatus(SpeculationStatus callerStatus) {
    this.specRpcFacade.setCallerStatus(callerStatus);
  }

  public void setCalleeStatus(SpeculationStatus calleeStatus) {
    this.specRpcFacade.setCalleeStatus(calleeStatus);
  }

  // This method should not be synchronized because CallbackRunner (Thread) holds
  // the monitor before it finishes the run() method.
  public SpeculationStatus getCallbackStatus() {
    return this.specRpcFacade.getCurrentRpcStatus();
  }

}
