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

package rc.client;

import rc.common.TxnReadResult;

public interface RcClientRpcFacade {
    
  /**
   * Invokes an RPC to the specified server to perform transaction read.
   * 
   * @param serverId
   * @param txnId
   * @param key
   * @return a transaction read result with read data if successfully. Otherwise,
   *         a result with a failure tag.
   */
  public TxnReadResult read(String serverId, String txnId, String key);
  
  /**
   * Invokes an RPC to the specified server to abort the txn.
   * 
   * @param serverId
   * @param txnId
   * @param readKeyList
   * @return True
   */
  public boolean abort(String serverId, String txnId, String[] readKeyList);
  
  /**
   * Invokes an RPC to the specified server to propose to commit the txn.
   * 
   * @param serverId
   * @param txnId
   * @param readKeyList
   * @param writeKeyList
   * @param writeValList
   * @return True if 2PC prepare-phase result is true. Otherwise, false.
   */
  public boolean proposeToCommitTxn(String serverId, String txnId, String[] readKeyList, String[] writeKeyList, String[] writeValList);
}
