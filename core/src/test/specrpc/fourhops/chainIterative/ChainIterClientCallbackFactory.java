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

package specrpc.fourhops.chainIterative;

import java.util.ArrayList;

import specrpc.client.api.SpecRpcCallbackObject;
import specrpc.client.api.SpecRpcCallbackFactory;
import specrpc.common.RpcSignature;

public class ChainIterClientCallbackFactory implements SpecRpcCallbackFactory {

  protected final int index;// callback index In Iterative Pattern
  protected final RpcSignature methodSignature;
  protected final String requestValue;
  protected final ArrayList<Object> predictedValues;

  public ChainIterClientCallbackFactory(int index, RpcSignature methodSignature, String requestValue,
      ArrayList<Object> predictedValues) {
    this.index = index;
    this.methodSignature = methodSignature;
    this.requestValue = requestValue;
    this.predictedValues = predictedValues;// may be null
  }

  public ChainIterClientCallbackFactory(int index, RpcSignature methodSignature, String requestValue,
      Object... predictedValues) {
    this.index = index;
    this.methodSignature = methodSignature;
    this.requestValue = requestValue;
    this.predictedValues = new ArrayList<Object>();
    for (Object value : predictedValues) {
      this.predictedValues.add(value);
    }
  }

  @Override
  public SpecRpcCallbackObject createCallback() {
    return new ChainIterClientCallback(this.index, this.methodSignature, this.requestValue, this.predictedValues);
  }

}
