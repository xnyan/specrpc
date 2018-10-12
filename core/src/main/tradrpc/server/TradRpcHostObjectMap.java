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

package tradrpc.server;

import java.util.HashMap;
import java.util.Map;

import rpc.execption.MethodNotRegisteredException;
import rpc.server.api.RpcHostObjectFactory;
import specrpc.common.RpcSignature;
import tradrpc.server.api.TradRpcHost;
import tradrpc.server.api.TradRpcHostFactory;

public class TradRpcHostObjectMap {
  private final Map<String, TradRpcHostFactory> directory = new HashMap<String, TradRpcHostFactory>();

  public void register(RpcSignature signature, RpcHostObjectFactory hostClassFactory) {
    directory.put(signature.toString(), (TradRpcHostFactory) hostClassFactory);
  }

  public TradRpcHost getHostObject(RpcSignature signature) throws MethodNotRegisteredException {
    TradRpcHostFactory factory = directory.get(signature.toString());
    if (factory == null) {
      throw new MethodNotRegisteredException(signature);
    }
    return factory.getRpcHostObject();
  }
}
