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

package specrpc.server.api;

public interface SpecRpcRollback {

  /**
   * Rollbacks the execution of an RPC or Callback instance.
   * 
   * Applications just need to register rollback functions to an RPC or Callback
   * instance instead of actively calling this function. SpecRPC will
   * automatically executes this function when speculation fails for the RPC or
   * Callback instance.
   * 
   * Applications should register rollback functions via the SpecRpcFacade
   * interface before any speculative RPC or Callback instance executes (or
   * triggers any speculation exception). SpecRPC currently suggests that
   * applications install rollback functions in the beginning of the execution of
   * each RPC or Callback.
   * 
   * If the application chooses to use this feature, SpecRPC assumes that the RPC
   * or Callback instance finishes its execution without using specBlock to hold
   * speculative execution. This means that the rollback function executes after
   * the RPC or Callback instance completes.
   * 
   * Instead of using this interface, an alternative way to rollback an RPC or
   * Callback is to either actively check speculative states or catch
   * SpeculationFailException and then performs rollback. This approach is more
   * fine-grained than using this rollback interface.
   */
  public void rollback();
}
