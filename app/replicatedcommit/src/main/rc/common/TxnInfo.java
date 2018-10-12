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

package rc.common;

import java.util.ArrayList;

/**
 * This class is used for dynamically constructing a txn information, e.g. a txn
 * coordinator dispatches read/write keys to related servers.
 * 
 * TODO Combines Txn and TxnInfo into one structure
 */
public class TxnInfo {
  
  public enum TXN_STATUS {
    INIT, PREPARED, ABORTED, COMMITTED
  };

  public final String txnId;
  private ArrayList<String> readKeyList;
  private ArrayList<String> writeKeyList;
  private ArrayList<String> writeValList;
  
  private TXN_STATUS txnStatus;

  public TxnInfo(String txnId) {
    this.txnId = txnId;
    this.readKeyList = new ArrayList<String>();
    this.writeKeyList = new ArrayList<String>();
    this.writeValList = new ArrayList<String>();
    this.txnStatus = TXN_STATUS.INIT;
  }
  
  public void setTxnStatus(TXN_STATUS status) {
    this.txnStatus = status;
  }
  
  public TXN_STATUS getTxnStatus() {
    return this.txnStatus;
  }

  public void addReadKey(String key) {
    this.readKeyList.add(key);
  }

  public void addWriteKeyVal(String key, String val) {
    this.writeKeyList.add(key);
    this.writeValList.add(val);
  }

  public String[] getReadKeyList() {
    return this.readKeyList.toArray(RcConstants.STRING_ARRAY_TYPE_HELPER);
  }

  public String[] getWriteKeyList() {
    return this.writeKeyList.toArray(RcConstants.STRING_ARRAY_TYPE_HELPER);
  }

  public String[] getWriteValList() {
    return this.writeValList.toArray(RcConstants.STRING_ARRAY_TYPE_HELPER);
  }
}
