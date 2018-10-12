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
import java.util.concurrent.CyclicBarrier;

import specrpc.client.api.SpecRpcCallbackObject;
import specrpc.client.api.SpecRpcCallbackFactory;

public class OneHopClientCallbackFactory implements SpecRpcCallbackFactory {

  protected int callbackCount;
  protected Vector<String> rpcReturnValues;
  protected CyclicBarrier allCallbacksBarrier;

  public OneHopClientCallbackFactory() {
    this.callbackCount = 0;
    this.rpcReturnValues = new Vector<String>();
  }

  public synchronized void setBarrier(int parties) {
    this.allCallbacksBarrier = new CyclicBarrier(parties);
  }

  public synchronized CyclicBarrier getBarrier() {
    return this.allCallbacksBarrier;
  }

  @Override
  public synchronized SpecRpcCallbackObject createCallback() {
    this.callbackCount++;
    return new OneHopClientCallback(this.rpcReturnValues, this.allCallbacksBarrier);
  }

  public synchronized void clearCallbackCount() {
    this.callbackCount = 0;
    this.rpcReturnValues.clear();
  }

  public synchronized int getCallbackCount() {
    return this.callbackCount;
  }

  public synchronized Vector<String> getRPCReturnValues() {
    return this.rpcReturnValues;
  }
}
