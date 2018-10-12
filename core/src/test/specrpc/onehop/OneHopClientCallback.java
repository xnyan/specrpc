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

package specrpc.onehop;

import java.util.Vector;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

import specrpc.client.api.SpecRpcCallbackObject;
import specrpc.exception.SpeculationFailException;

public class OneHopClientCallback extends SpecRpcCallbackObject {

  public static final String CALLBACK_VALUE_PREFIX = "Callback ";

  public static final long SLEEP_TIME = 5;

  protected final Vector<String> rpcReturnValues;
  protected final CyclicBarrier allCallbacksBarrier;

  public OneHopClientCallback(Vector<String> rpcReturnValues, CyclicBarrier allCallbacksBarrier) {
    this.rpcReturnValues = rpcReturnValues;
    this.allCallbacksBarrier = allCallbacksBarrier;
  }

  @Override
  public Object run(Object rpcReturnValue) throws SpeculationFailException, InterruptedException {
    this.rpcReturnValues.add((String) rpcReturnValue);
    try {
      this.allCallbacksBarrier.await();
    } catch (BrokenBarrierException e) {
      e.printStackTrace();
    }

    Thread.sleep(OneHopClientCallback.SLEEP_TIME);

    return CALLBACK_VALUE_PREFIX + rpcReturnValue;
  }
}
