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

import java.util.concurrent.ExecutorService;

import Waterloo.MultiSocket.ConnectionHandler;
import Waterloo.MultiSocket.IConnection;

public class SpecRpcConnectionHandler implements ConnectionHandler {

  private final ExecutorService serverThreadPool;
  private final SpecRpcHostObjectMap hostObjectMap;

  public SpecRpcConnectionHandler(ExecutorService serverThreadPool, SpecRpcHostObjectMap hostObjectMap) {
    this.serverThreadPool = serverThreadPool;
    this.hostObjectMap = hostObjectMap;
  }

  @Override
  public void handle(IConnection connection) {
    serverThreadPool.execute(new SpecRpcHandler(connection, hostObjectMap, serverThreadPool));
  }
}
