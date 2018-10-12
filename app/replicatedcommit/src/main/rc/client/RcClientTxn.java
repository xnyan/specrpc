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

import java.util.Hashtable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rc.client.txn.ClientTxnOperation;
import rc.client.txn.ReadFailedException;
import rc.client.txn.TxnException;
import rc.common.PaxosInstance;
import rc.common.RcConstants;
import rc.common.TxnReadResult;

// Not thread safe
public abstract class RcClientTxn {

  private static final Logger logger = LoggerFactory.getLogger(RcConstants.LOGGER_TYPE);
  
  public final String txnId;
  // Read <key, value> pairs that have acquired read locks on a quorum of replicas
  protected Hashtable<String, String> readKeyValTable;  // thread safe
  protected Hashtable<String, String> writeKeyValTable; // thread safe
  // A txn is terminated once the client calls to commit or abort the txn.
  protected boolean isTerminated;
  
  public RcClientTxn(String txnId) {
    this.txnId = txnId;
    this.readKeyValTable = new Hashtable<String, String>();
    this.writeKeyValTable = new Hashtable<String, String>();
    this.isTerminated = false;
  }
  
  private void checkIfTerminated() throws TxnException {
    if (this.isTerminated) {
      throw new TxnException("Txn has been committed or aborted. Txn id = " + this.txnId);
    }
  }

  /**
   * Performs a transaction read.
   * 
   * @param key
   * @return read value if available
   * @throws ReadFailedException
   */
  public String read(String key) throws ReadFailedException, TxnException {
    this.checkIfTerminated();
    if (key == null) {
      throw new TxnException("Invalid key = " + key);
    }
    
    if (this.writeKeyValTable.containsKey(key)) {
      return this.writeKeyValTable.get(key);
    }
    
    if (this.readKeyValTable.containsKey(key)) {
      return this.readKeyValTable.get(key);
    }
    
    TxnReadResult txnReadResult = this.doRead(key);
    
    if (! txnReadResult.isSharedLockAcquired) {
      throw new ReadFailedException("Transaction read failed on key = " + key + " because of failing to grab the read lock.");
    }
    
    // Cache read result
    if (txnReadResult.val == null) {
      this.readKeyValTable.put(key, RcConstants.READ_NULL_VALUE);
    } else {
      this.readKeyValTable.put(key, txnReadResult.val);
    }
    
    return txnReadResult.val;
  }
  
  /**
   * Invokes RPCs to all replicas to perform transaction read. Returns the read
   * value with the latest version from a quorum of response.
   * 
   * @param key
   * @return txnReadResult
   */
  protected TxnReadResult doRead(String key) throws TxnException {
    String[] serverIdList = RcClientLib.RC_SERVER_LOCATION_SERVICE.getServerIdList(key);
    ReadQuorum readQuorum = new ReadQuorum(RcClientLib.QUORUM_NUM);
    // Asynchronously calls RPCs to all participant servers in all DCs
    for (String serverId : serverIdList) {
      RcClientLib.THREAD_POOL.execute(new Runnable() {
        @Override
        public void run() {
          TxnReadResult readResult = RcClientLib.CLIENT_RPC_FACADE.read(serverId, txnId, key);
          if (readResult == null) {
            // Exception happens, treats as a failed lock.
            readQuorum.failLock();
          } else {
            if (readResult.isSharedLockAcquired) {
              readQuorum.acquireLock(readResult);
            } else {
              readQuorum.failLock();
            }
          }
          synchronized(readQuorum) {
            readQuorum.notifyAll();
          }
        }
      });
    }
    
    synchronized(readQuorum) {
      // Waits for a quorum of response
      while (readQuorum.isAcquireQuorumLocks() == null) {
        try {
          readQuorum.wait();
        } catch (InterruptedException e) {
          logger.error(e.getMessage());
          throw new TxnException(e.getMessage());
        }
      }
    }
    
    if (readQuorum.isAcquireQuorumLocks()) {
      // Read is successful
      return readQuorum.getTxnReadResult();
    } else {
      // Read fails
      return new TxnReadResult(null, null, false);
    }
    
  }

  /**
   * Performs transaction write. Buffers writes until commit.
   * 
   * @param key
   * @param val
   */
  public void write(String key, String val) throws TxnException {
    this.checkIfTerminated();
    // BUffers writes
    this.writeKeyValTable.put(key, val);
  }
  
  /**
   * Performs a transaction commit.
   * @return true if the transaction is committed. Otherwise, false.
   */
  public Boolean commit() throws TxnException {
    this.checkIfTerminated();
    this.isTerminated = true;
    
    // Determines the coordinator's serverIDs of the coordinator in each DC
    String key = null;
    if (! this.readKeyValTable.isEmpty()) {
      // Randomly picks a readKey
      key = this.readKeyValTable.keySet().iterator().next();// Note: not deterministic
    } else if (! this.writeKeyValTable.isEmpty()) {
      key = this.writeKeyValTable.keySet().iterator().next();
    } else {
      throw new TxnException("Txn has not perform any operation. Txn id = " + this.txnId);
    }
    
    String[] coordinatorServerIdList = RcClientLib.RC_SERVER_LOCATION_SERVICE.getServerIdList(key);
    // Note: toArray() does not have a deterministic return order
    String[] readKeyList = this.readKeyValTable.keySet().toArray(RcConstants.STRING_ARRAY_TYPE_HELPER);
    // Guarantees the order of the key and value lists.
    String[] writeKeyList = new String[this.writeKeyValTable.size()];
    String[] writeValList = new String[this.writeKeyValTable.size()];
    int i = 0;
    for (String writeKey : this.writeKeyValTable.keySet()) {
      writeKeyList[i] = writeKey;
      writeValList[i] = this.writeKeyValTable.get(writeKey);
      i++;
    }
    
    PaxosInstance paxosInstance = RcClientLib.createPaxosInstance();
    
    // Invokes RPCs to all coordinators in all DCs
    for (String serverId : coordinatorServerIdList) {
      RcClientLib.THREAD_POOL.execute(new Runnable() {
        @Override
        public void run() {
          boolean isAcceptToCommit = RcClientLib.CLIENT_RPC_FACADE.proposeToCommitTxn(
              serverId, txnId, readKeyList, writeKeyList, writeValList);
          if (isAcceptToCommit) {
            paxosInstance.voteAccept();
          } else {
            paxosInstance.voteReject();
          }
          synchronized(paxosInstance) {
            paxosInstance.notifyAll();
          }
        }
      });
    }
    
    synchronized(paxosInstance) {
      // Waits for Paxos consensus result
      while (paxosInstance.isAccept() == null) {
        try {
            paxosInstance.wait();
        } catch (InterruptedException e) {
          logger.error(e.getMessage());
          throw new TxnException(e.getMessage());
        }
      }
    }
    
    return paxosInstance.isAccept();
  }

  /**
   * Aborts the txn. At this point, the client must not 
   * call to commit the txn.
   * 
   * @throws TxnException
   */
  public void abort() throws TxnException {
    this.checkIfTerminated();
    this.isTerminated = true;
    
    /*
     * Clients DO NOT need to invoke RPCs to abort the txn because at this time the
     * txn only holds shared locks which will not block any concurrent txns. If a
     * txn tries acquire the exclusive locks, the shared locks are released
     * automatically.
     */
    return;
    
    /*
    //    // The following is an implementation of calling servers to abort the txn.
    //    if (this.readKeyValTable.isEmpty()) {
    //      return;
    //    }
    //    
    //    // Gets all participant servers in all DCs and the read key list on each server.
    //    HashMap<String, ArrayList<String>> serverIdReadKeyListMap = RcClientLib.RC_SERVER_LOCATION_SERVICE
    //        .mapReadKeysToShardServers(this.readKeyValTable.keySet().toArray(RcConstants.STRING_ARRAY_TYPE_HELPER));
    //    
    //    // Asynchronously aborts the txn on each server.
    //    // An alternative is to abort the txn through the coordinator in each DC.
    //    for (String serverId : serverIdReadKeyListMap.keySet()) {
    //      RcClientLib.THREAD_POOL.execute(new Runnable() {
    //        @Override
    //        public void run() {
    //          RcClientLib.CLIENT_RPC_FACADE.abort(serverId, txnId,
    //              serverIdReadKeyListMap.get(serverId).toArray(RcConstants.STRING_ARRAY_TYPE_HELPER));
    //        }
    //      });
    //    }
    */
  }
  
  /**
   * Executes a list of txn read/write operations.
   * 
   * @param txnOpList
   * @param startIndex
   * @throws TxnException
   */
  public void executeTxnOperations(ClientTxnOperation[] txnOpList, int startIndex) 
      throws ReadFailedException, TxnException {
    this.checkIfTerminated();
    this.doExecuteTxnOperations(txnOpList, startIndex);
  }
  
  // A sequential execution of read operations.
  protected void doExecuteTxnOperations(ClientTxnOperation[] txnOpList, int startIndex) 
      throws ReadFailedException, TxnException {
    if (txnOpList == null) {
      return;
    }
    
    for (int i = startIndex; i < txnOpList.length; i++) {
      switch(txnOpList[i].opType) {
      case READ:
        logger.debug("Txn id= " + this.txnId + " issues read for key= " + txnOpList[i].getKey());
        this.read(txnOpList[i].getKey());
        break;
      case WRITE:
        this.write(txnOpList[i].getKey(), txnOpList[i].getVal());
        break;
      default:
        throw new TxnException("Unknown txn operation, type = " + txnOpList[i].opType);
      }
    }
    
    if (logger.isDebugEnabled()) {
      this.debugTxnExecResult();
    }
  }
  
  /**
   * Executes the specified txn operations and tries to commit the txn.
   * Returns true if committed. Otherwise, false.
   * 
   * @param txnOpList
   * @return True if txn is committed. Otherwise, false.
   * @throws TxnException
   */
  public Boolean executeAndCommitTxn(ClientTxnOperation[] txnOpList) throws TxnException {
    try {
      this.executeTxnOperations(txnOpList, 0);
    } catch (ReadFailedException | TxnException e) {
      this.abort();
      return false;
    }
    return this.commit();
  }
  
  // For debug only
  protected void debugTxnExecResult() {
    if (logger.isDebugEnabled()) {
      // read keys
      for (String key : this.readKeyValTable.keySet()) {
        logger.debug("Txn id= " + this.txnId + " reads key= " + key + ", val= " + this.readKeyValTable.get(key));
      }
      // write keys
      for (String key : this.writeKeyValTable.keySet()) {
        logger.debug("Txn id= " + this.txnId + " writes key= " + key + ", val= " + this.writeKeyValTable.get(key));
      }
    }
  }
}
