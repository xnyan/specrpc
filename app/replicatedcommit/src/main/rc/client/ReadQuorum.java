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

public class ReadQuorum {
  
  private int acquiredLockNum; // Number of locks that are acquired.
  private int failedLockNum; // Number of locks that are failed to acquire.
  private final int quorumNum;
  private TxnReadResult txnReadResult;
  
  public ReadQuorum(int quorumNum) {
    this.acquiredLockNum = 0;
    this.failedLockNum = 0;
    this.quorumNum = quorumNum;
    this.txnReadResult = null;
  }
  
  public synchronized void failLock() {
    this.failedLockNum++;
  }
  
  /**
   * A read operation successfully acquires the shared lock.
   * Records the value with the newest version number.
   * 
   * @param readResult
   */
  public synchronized void acquireLock(TxnReadResult readResult) {
    this.acquiredLockNum++;
    if (this.txnReadResult == null) {
      this.txnReadResult = readResult;
      return;
    }
    
    if (readResult.version == null) {
      // Current read return does not have the key-value pair in the database
      return;
    }

    if (this.txnReadResult.version == null) {
      // Previous read return does not have the key-value pair in the database
      this.txnReadResult = readResult;
      return;
    }
    
    if (this.txnReadResult.version < readResult.version) {
      this.txnReadResult = readResult; 
    }
  }
  
  public synchronized Boolean isAcquireQuorumLocks() {
    if (this.acquiredLockNum >= this.quorumNum) {
      return true;
    }
    
    if (this.failedLockNum >= this.quorumNum) {
      return false;
    }
    
    return null;
  }
  
  /**
   * Returns true if there has been any read return. Otherwise, returns false.
   * @return true if there has been any read return. False if there is no return.
   */
  public synchronized boolean isAnyReadReturn() {
    if (this.acquiredLockNum == 0 && this.failedLockNum == 0) {
      // No read return.
      return false;
    }
    return true;
  }
  
  /**
   * Returns the txn read result. 
   * @return txn read result if read is successful. Otherwise, null.
   */
  public synchronized TxnReadResult getTxnReadResult() {
    return this.txnReadResult;
  }
}
