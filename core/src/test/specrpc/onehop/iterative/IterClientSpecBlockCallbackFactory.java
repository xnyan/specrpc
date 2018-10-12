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

package specrpc.onehop.iterative;

import java.util.ArrayList;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicInteger;

import specrpc.client.api.SpecRpcCallbackObject;
import specrpc.common.RpcSignature;

public class IterClientSpecBlockCallbackFactory extends IterClientCallbackFactory {

  protected final CyclicBarrier allCallbacksBarrier;
  protected final AtomicInteger passSpecBlockNum;
  protected final AtomicInteger failSpecBlockNum;

  public IterClientSpecBlockCallbackFactory(int index, RpcSignature methodSignature, String requestValue,
      ArrayList<Object> predictedValues, CyclicBarrier allCallbacksBarrier, AtomicInteger passSpecBlockNum,
      AtomicInteger failSpecBlockNum) {
    super(index, methodSignature, requestValue, predictedValues);
    this.allCallbacksBarrier = allCallbacksBarrier;
    this.passSpecBlockNum = passSpecBlockNum;
    this.failSpecBlockNum = failSpecBlockNum;
  }

  @Override
  public SpecRpcCallbackObject createCallback() {
    return new IterClientSpecBlockCallback(this.index, this.methodSignature, this.requestValue, this.predictedValues,
        this.allCallbacksBarrier, this.passSpecBlockNum, this.failSpecBlockNum);
  }

}
