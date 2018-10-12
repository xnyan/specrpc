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

package rc.server.db.lock;

import java.util.HashSet;
import java.util.Hashtable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rc.common.RcConstants;
import rc.common.Utils;

/**
 * Locking rules in the ReplicatedCommit protocol as per the paper:
 * 1. "An exclusive lock request can take over an existing shared lock"
 * 2. "Denies a conflicting shared and exclusive locks"  
 * 
 * Prepare-phase locking rules in the ReplicatedCommit protocol as per the paper:
 * 1. Acquires exclusive locks for locks for write operations
 * 2. Verifies if still holding shared locks for read operations
 * 3. Logs the prepare operation in to the TxnLog
 * 4. Once a prepare operation has been locked, its shared locks can be released.
 * 
 * Commit-phase locking rules in the ReplicatedCOmmit protocol as per the paper:
 * 1. Performs updates
 * 2. Logs the commit operation in to the TxnLog
 * 3. Releases locks
 */
public class LockTable {
 
  private static final Logger logger = LoggerFactory.getLogger(RcConstants.LOGGER_TYPE);
  
  /**
   *  Maps of <key, list[txnId]>
   *  An entry means that a list of txns hold the acquired a shared (read) lock on the key.
   */
  private Hashtable<String, HashSet<String>> sharedLockTable;
  
  /**
   * Maps of <key, txnId>
   * An entry means that the txn holds the exclusive (write) lock on the key.
   */
  private Hashtable<String, String> exclusiveLockTable;
  
  private ConcurrentHashSet<String> preparedTxnSet;
  
  public LockTable () {
    this.sharedLockTable = new Hashtable<String, HashSet<String>>();
    this.exclusiveLockTable = new Hashtable<String, String>();
    this.preparedTxnSet = new ConcurrentHashSet<String>();
  }
  
  public ConcurrentHashSet<String> getPreparedTxnSet() {
    return this.preparedTxnSet;
  }
  
  // Only to be called as a helper function. No need to be thread safe.
  private Boolean isExclusiveLockHeldByAnotherTxn(String txnId, String key) {
    if (this.exclusiveLockTable.containsKey(key)) {
      if (! this.exclusiveLockTable.get(key).equals(txnId)) {
        // The exclusive lock on the key is held by another txn
        return true;
      }
    }
    return false;
  }
  
  public synchronized Boolean acquireSharedLock(String txnId, String key) {
    if (this.isExclusiveLockHeldByAnotherTxn(txnId, key)) {
      return false;
    }
    // Grants the shared lock
    HashSet<String> sharedLockTxnList = this.sharedLockTable.get(key);
    if (sharedLockTxnList == null) {
      sharedLockTxnList = new HashSet<String>();
      sharedLockTxnList.add(txnId);
      this.sharedLockTable.put(key, sharedLockTxnList);
    } else if (! sharedLockTxnList.contains(txnId)) {
      sharedLockTxnList.add(txnId);
    }
    
    return true;
  }
  
  public synchronized void releaseSharedLock(String txnId, String key) {
    if (! this.sharedLockTable.containsKey(key)) {
      // No txn holds the shared lock on the key
      return;
    }
    
    HashSet<String> txnIdSet = this.sharedLockTable.get(key);
    if (! txnIdSet.contains(txnId)) {
      // The given txn does not hold the shared lock on the key
      return;
    }
    
    // Releases the shared lock for the given txn
    txnIdSet.remove(txnId);
    
    // Removes the key from the shared lock table if there is no shared lock held on the key
    if (txnIdSet.isEmpty()) {
      this.sharedLockTable.remove(key);
    }
  }
  
  /**
   * Preemptively take over conflicting shared locks if any regardless of if
   * conflicting txns are PREPARED or not.
   * 
   * @param txnId
   * @param key
   */
  private void monopolizeSharedLock(String txnId, String key) {
    if (this.sharedLockTable.containsKey(key)) {
      HashSet<String> sharedLockTxnList = this.sharedLockTable.get(key);
      // Takes over the shared lock from other txns
      if (sharedLockTxnList.contains(txnId)) {
        // Only the given txn holds the shared lock.
        sharedLockTxnList.clear();
        sharedLockTxnList.add(txnId);
      } else {
        // Removes the key from the shared lock table if the given txn does not need the shared lock.
        this.sharedLockTable.remove(key);
      }
    }
  }
  
  /**
   * Tries to take over conflicting shared locks. Can not take over
   * the shared lock if it is acquired by a PREATED txn.
   * Return true if successful. Otherwise, false.
   * 
   * @param txnId
   * @param key
   */
  private boolean tryToTakeOverSharedLock(String txnId, String key) {
    if (this.sharedLockTable.containsKey(key)) {
      HashSet<String> sharedLockTxnList = this.sharedLockTable.get(key);
      for (String id : sharedLockTxnList) {
        if (! id.equals(txnId)) {
          if (this.preparedTxnSet.contains(id)) {
            // There is one PREPARED txn holding the shared lock 
            
            if (logger.isDebugEnabled()) {
              logger.debug("The shared lock on the key = " + key +
                  " is held by a PREPARED txn id = " + id +
                  ". The shared lock can not be taken over by txn id = " + txnId);
            }
            
            return false;
          }
        }
      }
      
      // No prepared txn holds the shared lock, preemptively takes over the lock.
      this.monopolizeSharedLock(txnId, key);
    }
    return true;
  }
  
  private Boolean acquireExclusiveLock(String txnId, String key) {
    if (this.isExclusiveLockHeldByAnotherTxn(txnId, key)) {
      
      if (logger.isDebugEnabled()) {
        logger.debug("The exclusive lock on key = " + key +
            " is held by a txn other than txn id = " + txnId);
      }
      
      return false;
    }
    // Grants exclusive lock
    if (! this.exclusiveLockTable.containsKey(key)) {
      this.exclusiveLockTable.put(key, txnId);
    }
    
    //Try to take over shared locks from conflicting txns if any
    return this.tryToTakeOverSharedLock(txnId, key);
  }
  
  private void releaseExclusiveLock(String txnId, String key) {
    if (! this.exclusiveLockTable.containsKey(key)) {
      return;
    }
    
    if (! this.exclusiveLockTable.get(key).equals(txnId)) {
      // Can not release the exclusive lock if the given txn does not hold the lock
      return;
    }
    
    this.exclusiveLockTable.remove(key);
  }
  
  private Boolean verifyHoldingSharedLock(String txnId, String[] readKeyList) {
    if (readKeyList == null || readKeyList.length == 0) {
      return true;
    }
    for (String key : readKeyList) {
      if (! this.sharedLockTable.containsKey(key)) {
        
        if (logger.isDebugEnabled()) {
          logger.debug("The shared lock on the key is NOT held by any txn.");
        }
        
        return false;
      } else if (! this.sharedLockTable.get(key).contains(txnId)) {
        
        if (logger.isDebugEnabled()) {
          logger.debug("The shared lock on the key is NOT held by the txn id = " + txnId);
        }
        
        return false;
      }
    }
    return true;
  }
  
  /**
   * Acquires required exclusive locks. If failed, return false.
   * Verifies still holding shared locks. If failed, return false.
   * Locks the txn as prepared.
   * 
   * If return false, the txn may still hold some exclusive and shared locks.
   * 
   * Note: verifying holding shared locks and locking a prepared operation must be atomic.
   * Otherwise, shared locks may be taken over by other txns' exclusive lock requests before
   * the prepared operation is locked.
   * @param txnId
   * @param writeKeyList
   * @param readKeyList
   * @return true if a txn is prepared, otherwise false
   */
  public synchronized Boolean prepareTxn(String txnId, String[] writeKeyList, String[] readKeyList) { 
    /*
     * TODO Optimization
     * 
     * 1. Check if any prepared txn holds a conflicting shared lock. If yes, do not
     * try to acquire any exclusive lock. If the current txn is determined to fail
     * acquiring the exclusive lock, it will not take over any shared lock which may
     * lead to aborting other txns.
     * 
     * 2. Similarly checks conflicting exclusive locks first.
     * 
     * 3. Releases any acquired exclusive locks if failed. But this will block
     * non-concurrent txns since lock table is thread safe.
     * 
     * 3. Decouples acquiring all exclusive locks from this method?
     */
    // Acquires all exclusive locks 
    if (writeKeyList != null && writeKeyList.length != 0) {
      for (int i = 0; i < writeKeyList.length; i++) {
        if (! this.acquireExclusiveLock(txnId, writeKeyList[i])) {
          
          if (logger.isDebugEnabled()) {
            logger.debug("Failed to acquire exclusive lock on key = " + writeKeyList[i] + 
                " for txn id = " + txnId);
          }
          
          return false;
        }
      }
    }
    
    // Verifies if holding shared locks
    if (! this.verifyHoldingSharedLock(txnId, readKeyList)) {
      
      if (logger.isDebugEnabled()) {
        logger.debug("Txn id = " + txnId + 
            " does NOT hold all shared locks on read-key list = " + Utils.toString(readKeyList));
      }
      
     return false; 
    }
    
    // Locks the prepared txn
    this.preparedTxnSet.add(txnId);
    
    return true;
  }
    
  public synchronized void releaseMultiSharedLocks(String txnId, String[] readKeyList) {
    if (readKeyList == null || readKeyList.length == 0) {
      return;
    }
    for (int i = 0; i < readKeyList.length; i++) {
      this.releaseSharedLock(txnId, readKeyList[i]);
    }
  }
  
  public synchronized void releaseMultiExclusiveLocks(String txnId, String[] writeKeyList) {
    if (writeKeyList == null || writeKeyList.length == 0) {
      return;
    }
    this.releaseMultiExclusiveLocks(txnId, writeKeyList, 0, writeKeyList.length);
  }
  
  private void releaseMultiExclusiveLocks(String txnId, String[] writeKeyList, int beginIndex, int endIndex) {
    if (writeKeyList == null || writeKeyList.length == 0) {
      return;
    }
    for (int i = beginIndex; i < endIndex; i++) {
      this.releaseExclusiveLock(txnId, writeKeyList[i]);
    }
  }

}
