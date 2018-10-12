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

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicInteger;

import specrpc.client.api.SpecRpcCallbackObject;
import specrpc.exception.SpeculationFailException;

public class ChainClientSpecBlockCallback extends SpecRpcCallbackObject {

  protected final CyclicBarrier allCallbacksBarrier;
  protected final AtomicInteger passSpecBlockNum;
  protected final AtomicInteger failSpecBlockNum;

  public ChainClientSpecBlockCallback(CyclicBarrier allCallbacksBarrier, AtomicInteger passSpecBlockNum,
      AtomicInteger failSpecBlockNum) {
    this.allCallbacksBarrier = allCallbacksBarrier;
    this.passSpecBlockNum = passSpecBlockNum;
    this.failSpecBlockNum = failSpecBlockNum;
  }

  @Override
  public Object run(Object rpcReturnValue) throws SpeculationFailException, InterruptedException {
    try {
      specRPCFacade.specBlock();
      this.passSpecBlockNum.incrementAndGet();
      this.allCallbacksBarrier.await();
    } catch (BrokenBarrierException e) {
      e.printStackTrace();
    } catch (SpeculationFailException e) {
      try {
        this.failSpecBlockNum.incrementAndGet();
        this.allCallbacksBarrier.await();
        throw e;
      } catch (BrokenBarrierException e1) {
        e1.printStackTrace();
      }
    }

    return rpcReturnValue;
  }
}
