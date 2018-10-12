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

package rc.server.db;

import rc.common.TxnInfo.TXN_STATUS;

/**
 * This class is used to record a txn information in a static way, i.e. knowing all read/write keys at initiation.
 * One example of the use of this class is logging a txn.
 */
public class Txn {

  public static final String REGEX = "=";
  public static final String TXN_ID_TAG = "txn-id";
  public static final String TXN_STATUS_TAG = "txn-status";
  public static final String TXN_READ_TAG = "txn-read";
  public static final String TXN_WRITE_TAG = "txn-write";
  public static final String TXN_VAL_TAG = "txn-val";

  public final String txnId;
  public final String[] readKeyList;
  public final String[] writeKeyList;
  public final String[] writeValList;

  private TXN_STATUS txnStatus;

  public Txn(
      String txnId,
      String[] readKeyList, 
      String[] writeKeyList, 
      String[] writeValList, 
      TXN_STATUS txnStatus) {
    this.txnId = txnId;
    this.readKeyList = readKeyList;
    this.writeKeyList = writeKeyList;
    this.writeValList = writeValList;
    this.txnStatus = txnStatus;
  }

  public synchronized TXN_STATUS getTxnStatus() {
    return this.txnStatus;
  }

  public synchronized void setTxnStatus(TXN_STATUS txnStatus) {
    this.txnStatus = txnStatus;
  }
  
  /**
   * Note: not thread safe
   * 
   * Format:
   * TXN_ID_TAG REGEX txnId
   * TXN_STATUS_TAG REGEX txnStatus
   * TXN_READ_TAG REGEX readKey1
   * TXN_READ_TAG REGEX readKey2
   * ...
   * TXN_WRITE_TAG REGEX writeKey1
   * TXN_VAL_TAG REGEX writeVal1
   * TXN_WRITE_TAG REGEX writeKey2
   * TXN_VAL_TAG REGEX writeVal2
   * ...
   */
  public String createLogInfo() {
    String res = TXN_ID_TAG + REGEX + this.txnId + "\n" +
        TXN_STATUS_TAG + REGEX + this.txnStatus + "\n";
    if (this.readKeyList != null && this.readKeyList.length != 0) {
      for (String readKey : this.readKeyList) {
        res += TXN_READ_TAG + REGEX + readKey + "\n";
      }
    }
    
    if (this.writeKeyList != null && this.writeKeyList.length != 0) {
      for (int i = 0; i < this.writeKeyList.length; i++) {
        res += TXN_WRITE_TAG + REGEX + this.writeKeyList[i] + "\n";
        res += TXN_VAL_TAG + REGEX + this.writeValList[i] + "\n";
      }
    }
    
    return res;
  }
}
