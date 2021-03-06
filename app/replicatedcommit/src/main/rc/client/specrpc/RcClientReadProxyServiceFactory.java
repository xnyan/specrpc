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

package rc.client.specrpc;

import specrpc.server.api.SpecRpcHost;
import specrpc.server.api.SpecRpcHostFactory;

public class RcClientReadProxyServiceFactory implements SpecRpcHostFactory {

  @Override
  public String getRpcHostClassName() {
    return RcClientReadProxyService.class.getName();
  }

  @Override
  public SpecRpcHost getRpcHostObject() {
    return new RcClientReadProxyService();
  }

}
