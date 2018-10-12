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

import specrpc.client.api.SpecRpcCallbackObject;
import specrpc.common.RpcSignature;

public class IterClientExceptionCallbackFactory extends IterClientCallbackFactory {

  public IterClientExceptionCallbackFactory(int index, RpcSignature methodSignature, String requestValue,
      ArrayList<Object> predictedValues) {
    super(index, methodSignature, requestValue, predictedValues);
  }

  @Override
  public SpecRpcCallbackObject createCallback() {
    return new IterClientExceptionCallback(this.index, this.methodSignature, this.requestValue, this.predictedValues);
  }

}
