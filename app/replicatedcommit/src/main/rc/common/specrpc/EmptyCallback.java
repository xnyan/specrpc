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

package rc.common.specrpc;

import specrpc.client.api.SpecRpcCallback;
import specrpc.common.api.SpecRpcFacade;
import specrpc.exception.SpeculationFailException;

public class EmptyCallback implements SpecRpcCallback {

  private SpecRpcFacade specRpcFacade;
  
  @Override
  public void bind(SpecRpcFacade specRpcFacade) {
    this.specRpcFacade = specRpcFacade;
  }

  @Override
  public Object run(Object rpcReturnValue) throws SpeculationFailException, InterruptedException {
    return rpcReturnValue;
  }

}
