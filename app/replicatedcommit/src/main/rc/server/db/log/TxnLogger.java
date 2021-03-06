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

package rc.server.db.log;

public interface TxnLogger {
  
  /**
   * Logs a txn prepare operation if the txn is prepared.
   * 
   * @param txnId
   * @param readKeyList
   * @param writeKeyList
   * @param writeValList
   * @return True if log is successful. Otherwise, false.
   */
  public boolean logPreparedTxn(String txnId, String[] readKeyList, String[] writeKeyList, String[] writeValList);
  
  /**
   * Log a txn commit operation. We do not need to log read information for reply.
   * 
   * @param txnId
   * @param writeKeyList
   * @param writeValList
   * @return True if log is successful. Otherwise, false.
   */
  public boolean logCommittedTxn(String txnId, String[] writeKeyList, String[] writeValList);
  
  public boolean logAbortedTxn(String txnId);
  
  public void shutdown();
}
