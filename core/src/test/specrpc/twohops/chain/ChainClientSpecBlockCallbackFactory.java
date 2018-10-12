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

package specrpc.twohops.chain;

import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicInteger;

import specrpc.client.api.SpecRpcCallbackObject;
import specrpc.client.api.SpecRpcCallbackFactory;

public class ChainClientSpecBlockCallbackFactory implements SpecRpcCallbackFactory {

  protected int callbackCount;
  protected CyclicBarrier allCallbacksBarrier;
  protected AtomicInteger passSpecBlockNum;
  protected AtomicInteger failSpecBlockNum;

  public ChainClientSpecBlockCallbackFactory() {
    this.passSpecBlockNum = new AtomicInteger(0);
    this.failSpecBlockNum = new AtomicInteger(0);
  }

  public synchronized void setBarrier(int parties) {
    this.allCallbacksBarrier = new CyclicBarrier(parties);
  }

  public synchronized CyclicBarrier getBarrier() {
    return this.allCallbacksBarrier;
  }

  public synchronized int getCallbackCount() {
    return this.callbackCount;
  }

  public synchronized int getPassSpecBlockNum() {
    return this.passSpecBlockNum.get();
  }

  public synchronized int getFailSpecBlockNum() {
    return this.failSpecBlockNum.get();
  }

  @Override
  public synchronized SpecRpcCallbackObject createCallback() {
    this.callbackCount++;
    return new ChainClientSpecBlockCallback(this.allCallbacksBarrier, this.passSpecBlockNum, this.failSpecBlockNum);
  }
}
