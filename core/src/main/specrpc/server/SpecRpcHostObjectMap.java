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

package specrpc.server;

import java.util.HashMap;
import java.util.Map;

import rpc.execption.MethodNotRegisteredException;
import rpc.server.api.RpcHostObjectFactory;
import specrpc.common.RpcSignature;
import specrpc.server.api.SpecRpcHost;
import specrpc.server.api.SpecRpcHostFactory;

public class SpecRpcHostObjectMap {

  private final Map<String, SpecRpcHostFactory> rpcSigToHostMap = new HashMap<String, SpecRpcHostFactory>();

  public void register(RpcSignature signature, RpcHostObjectFactory hostClassFactory) {
    this.rpcSigToHostMap.put(signature.toString(), (SpecRpcHostFactory) hostClassFactory);
  }

  public SpecRpcHost getHostObject(RpcSignature signature) throws MethodNotRegisteredException {
    SpecRpcHostFactory factory = this.rpcSigToHostMap.get(signature.toString());
    if (factory == null) {
      throw new MethodNotRegisteredException(signature);
    }
    return factory.getRpcHostObject();
  }
}
