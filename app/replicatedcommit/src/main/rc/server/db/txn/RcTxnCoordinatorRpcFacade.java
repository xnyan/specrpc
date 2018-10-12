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

package rc.server.db.txn;

import rc.common.TxnInfo;

public interface RcTxnCoordinatorRpcFacade {

  /**
   * Invokes an RPC to make the specified server prepare to commit the given txn.
   * 
   * @param serverId
   * @param txnInfo
   * @return True if the txn is prepared on the specified server, otherwise false.
   */
  public abstract Boolean prepareTxn(String serverId, TxnInfo txnInfo);

  /**
   * Invokes an RPC to make the specified server abort the given txn.
   * 
   * @param serverId
   * @param txnId
   * @return True
   */
  public abstract Boolean abortTxn(String serverId, String txnId);

  /**
   * Invokes an RPC to make the specified server commit the given txn.
   * 
   * @param serverId
   * @param txnId
   * @return
   */
  public abstract Boolean commitTxn(String serverId, String txnId);

  /**
   * Invokes an RPC to make the specified server knows the vote about if to commit the specified txn.
   * 
   * @param serverId
   * @param isVoteToCommit
   * @return
   */
  public abstract Boolean voteToCommitTxn(String serverId, String txnId, Boolean isVoteToCommit);

}
