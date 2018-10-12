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

import java.util.HashMap;

import rc.common.PaxosInstance;

public class RcTxnPaxosMap {

  public final int quorumNum;
  private HashMap<String, PaxosInstance> txnPaxosMap;

  public RcTxnPaxosMap(int quorumNum) {
    this.quorumNum = quorumNum;
    this.txnPaxosMap = new HashMap<String, PaxosInstance>();
  }

  public synchronized PaxosInstance getTxnPaxosInstance(String txnId) {
    if (!txnPaxosMap.containsKey(txnId)) {
      PaxosInstance paxosInstance = new PaxosInstance(this.quorumNum);
      txnPaxosMap.put(txnId, paxosInstance);
    }

    return txnPaxosMap.get(txnId);
  } 
}
