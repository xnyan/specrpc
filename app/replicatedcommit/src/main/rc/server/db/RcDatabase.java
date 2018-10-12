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

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Hashtable;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rc.common.RcConstants;
import rc.common.TxnInfo;
import rc.common.TxnReadResult;
import rc.common.Utils;
import rc.server.db.lock.LockTable;
import rc.server.RcServer;
import rc.server.db.lock.ConcurrentHashSet;
import rc.server.db.log.TxnLogger;
import rc.server.db.storage.DataValue;
import rc.server.db.storage.KeyValStore;

public class RcDatabase {
  
  private static final Logger logger = LoggerFactory.getLogger(RcConstants.LOGGER_TYPE);

  private KeyValStore keyValStore;
  private TxnLogger txnLog;
  private LockTable lockTable;
  private final ConcurrentHashSet<String> preparedTxnIdSet;
  private final Hashtable<String, Txn> preparedTxnTable;
  // A txn may not be PREPARED on a participant server but later will be committed by
  // the txn coordinator because of Paxos consensus.
  private final Hashtable<String, Txn> nonPreparedTxnTable;
  // A set of txns that are aborted by the txn coordinator.
  private final ConcurrentHashSet<String> abortedTxnIdSet;
  // A set of txns that are committed by the txn coordinator, but not actually committed.
  private final ConcurrentHashSet<String> toCommitTxnSet;
  // TODO deal with duplicate txn operations, e.g. abort requests on committed txns
  
  public RcDatabase(Properties serverConfig) {
    // Data storage & txn logger initialization
    String dataStoreClassName = serverConfig.getProperty(
        RcConstants.DB_STORAGE_CLASS_PROPERTY,
        RcConstants.DEFAULT_DB_STORAGE_CLASS);
    logger.debug("Database storage is " + dataStoreClassName);

    String txnLoggerClassName = serverConfig.getProperty(
        RcConstants.DB_TXN_LOG_CLASS_PROPERTY,
        RcConstants.DEFAULT_DB_TXN_LOG_CLASS);
    logger.debug("Database txn logger is " + txnLoggerClassName);
    
    try {
      // Only for BerkleyDB
      String bdbEnvHome=serverConfig.getProperty(RcConstants.DB_STORAGE_BDB_ENV_HOME_PROPERTY) + 
          RcConstants.DB_STORAGE_BDB_ENV_HOME_PREFIX + serverConfig.getProperty(RcConstants.SERVER_ID_PROPERTY);
      serverConfig.setProperty(RcConstants.DB_STORAGE_BDB_ENV_HOME_PROPERTY, bdbEnvHome);
      
      this.keyValStore = 
          (KeyValStore)
          Class
          .forName(dataStoreClassName)
          .getConstructor(Properties.class)
          .newInstance(serverConfig);
      this.txnLog =
          (TxnLogger)
          Class
          .forName(txnLoggerClassName)
          .getConstructor(Properties.class)
          .newInstance(serverConfig);
    } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
        | NoSuchMethodException | SecurityException | ClassNotFoundException e) {
      logger.error(e.getMessage());
      e.printStackTrace();
      System.exit(RcConstants.INIT_FAIL_ERROR_CODE);
    }

    // Lock table
    this.lockTable = new LockTable();
    // Uses the same locked prepared txn set as the lock table, but does not have contention on the lock table.
    this.preparedTxnIdSet = this.lockTable.getPreparedTxnSet();
    this.preparedTxnTable = new Hashtable<String, Txn>();
    this.nonPreparedTxnTable = new Hashtable<String, Txn>();
    this.abortedTxnIdSet = new ConcurrentHashSet<String>();
    this.toCommitTxnSet = new ConcurrentHashSet<String>();
    
    // Loads init data if any
    long initDataStartTime = 0;
    if (logger.isDebugEnabled()) {
      initDataStartTime = System.currentTimeMillis();
      logger.debug("Loading init data.");
    }
    String dataFile = serverConfig.getProperty(RcConstants.DATA_KEY_FILE_PROPERTY);
    BufferedReader dataReader = null;
    if (dataFile != null) {
      try {
        dataReader = new BufferedReader(new FileReader(dataFile));
        int keyNum = Integer.parseInt(dataReader.readLine());
        for (int i = 0; i < keyNum; i++) {
          String key = dataReader.readLine();
          if (key == null) {
            logger.error("Invalid data file format: finished reading " + i + " records but expecting " + keyNum);
            System.exit(RcConstants.INIT_FAIL_ERROR_CODE);
          }
          
          if(RcServer.SHARD_ID.equals(RcServer.RC_SERVER_LOCATION_SERVICE.getShardId(key))) {
            this.keyValStore.write(key, "0");
          }
        }
      } catch (FileNotFoundException e) {
        logger.error("Can not find init data file: " + dataFile);
        e.printStackTrace();
        System.exit(RcConstants.INIT_FAIL_ERROR_CODE);
      } catch (NumberFormatException e) {
        logger.error("Invalid data file format: first line should be the total number of data records.");
        e.printStackTrace();
        System.exit(RcConstants.INIT_FAIL_ERROR_CODE);
      } catch (IOException e) {
        logger.error(e.getMessage());
        e.printStackTrace();
        System.exit(RcConstants.INIT_FAIL_ERROR_CODE);
      }
    } else {
      logger.warn("No specified init data file.");
    }
    
    // Persists the initial data
    this.sync();
    
    if (logger.isDebugEnabled()) {
      long initDataEndTime = System.currentTimeMillis();
      logger.debug("Finished loading init data. Total time cost= " + (initDataEndTime - initDataStartTime) + " ms");
    }
  }

  /**
   * If acquiring read lock successfully, returns read value. Otherwise, returns
   * null as the read value and marks lock not acquired.
   * 
   * @param txnId
   * @param readKey
   * @return readVal
   */
  public TxnReadResult read(String txnId, String key) {
    TxnReadResult res = null;
    if (this.lockTable.acquireSharedLock(txnId, key)) {
      DataValue data = this.keyValStore.read(key);
      if (data == null) {
        res = new TxnReadResult(null, null, true);
      } else {
        res = new TxnReadResult(data.val, data.version, true);
      }
    } else {
      res = new TxnReadResult(null, null, false);
    }
    
    if (logger.isDebugEnabled()) {
      logger.debug("Read txn id = " + txnId +
          ", key = " + key +
          ", val = " + res.val +
          ", version = " + res.version +
          ", isSharedLockAcquired = " + res.isSharedLockAcquired);
    }
        
    return res;
  }

  /**
   * Prepare-phase rules in the ReplicatedCommit protocol as per the paper:
   * 
   * 1. Acquires exclusive locks for locks for write operations
   * 
   * 2. Verifies if still holding shared locks for read operations
   * 
   * 3. Logs the prepare operation in to the TxnLog
   * 
   * 4. Once a prepare operation has been locked, its shared locks can be
   * released.
   * 
   * @param txnId
   * @param readKeyList
   * @param writeKeyList
   * @param writeValList
   * @return true if the txn is prepared, otherwise false
   */
  public Boolean prepareTxn(String txnId, String[] readKeyList, String[] writeKeyList, String[] writeValList) {
    boolean isPrepared = this.lockTable.prepareTxn(txnId, writeKeyList, readKeyList);
    
    if (logger.isDebugEnabled()) {
      logger.debug("Prepare txn id = " + txnId +
          ", isPrepared = " + isPrepared +
          ", readKeyList = " + Utils.toString(readKeyList) +
          ", writeKeyLIst = " + Utils.toString(writeKeyList));
    }
    
    /*
     * It is possible that the coordinator's abort or commit request executes before
     * the prepare request. For example, the coordinator aborts the txn because of
     * another participant server can not prepare to commit the txn, and a Paxos
     * consensus is achieved on the abort decision among DCs. In this case, the
     * coordinator sends abort request to every participant servers, which may cause
     * the abort request to execute before this prepare request.
     */
    
    // TODO Asynchronously releases locks
    
    // Checks if the txn has been aborted by the coordinator.
    if (this.abortedTxnIdSet.contains(txnId)) {
      // The coordinator's abort request executes before this prepare request does.
      isPrepared = false;
      this.preparedTxnIdSet.remove(txnId);
      this.lockTable.releaseMultiExclusiveLocks(txnId, writeKeyList);
    } else {
      if (isPrepared) {
        this.preparedTxnTable.put(txnId, new Txn(txnId, readKeyList, writeKeyList, writeValList, TxnInfo.TXN_STATUS.PREPARED));
        this.txnLog.logPreparedTxn(txnId, readKeyList, writeKeyList, writeValList);
      } else {
        // Prepare failed
        // Releases all acquired exclusive locks
        this.lockTable.releaseMultiExclusiveLocks(txnId, writeKeyList);
        // Records the txn information in case the txn is to be committed by the coordinator.
        this.nonPreparedTxnTable.put(txnId,
            new Txn(txnId, readKeyList, writeKeyList, writeValList, TxnInfo.TXN_STATUS.PREPARED));
      }
    }
    // Releases all acquired shared locks
    this.lockTable.releaseMultiSharedLocks(txnId, readKeyList);
    
    // Checks if the txn has been determined to commit by the coordinator
    // Note: a txn must not be in both abort and commit sets.
    if (this.toCommitTxnSet.contains(txnId)) {
      this.doCommitTxn(txnId); // Tries to commit the txn.
    }
        
    return isPrepared;
  }
  
  /**
   * This method is for SpecRPC since incorrectly speculative read may have
   * acquired some shared locks that should be released at prepare phase.
   * Otherwise, these shared locks may cause concurrent txns to abort.
   * 
   * Releases the non-required shared locks, and prepares the txn.
   * 
   * @param txnId
   * @param readKeyList
   * @param writeKeyList
   * @param writeValList
   * @param nonRequciredReadKeyList
   * @return True if the txn is PREPARED. Otherwise, false.
   */
  public Boolean prepareTxnAfterSpec(String txnId, String[] readKeyList, String[] writeKeyList, String[] writeValList,
      String[] nonRequiredReadKeyList) {

    if (nonRequiredReadKeyList != null && nonRequiredReadKeyList.length != 0) {
      // Avoids to block on the lock table
      this.lockTable.releaseMultiSharedLocks(txnId, nonRequiredReadKeyList);
    }

    return this.prepareTxn(txnId, readKeyList, writeKeyList, writeValList);
  }
  
  /**
   * Aborts a txn when a client calls to abort the txn. This can only happen
   * before the client calls commit, which makes a transaction to be prepared.
   * 
   * @param txnId
   * @param readKeyList
   * @return True if aborting the txn successfully.
   */
  public Boolean clientAbortTxn(String txnId, String[] readKeyList) {
    // Txn must not be prepared as the coordinator will not start 2PC
    this.lockTable.releaseMultiSharedLocks(txnId, readKeyList);
    this.abortedTxnIdSet.add(txnId);
    
    return true;
  }

  /**
   * Aborts a txn when the coordinator calls to abort the txn. The txn may or may
   * not be PREPARED yet.
   * 
   * @param txnId
   * @return True if aborting the txn successfully
   */
  public Boolean abortTxn(String txnId) {
    
    this.abortedTxnIdSet.add(txnId); // Marks the txn as aborted
    
    // The prepare-phase may not happen yet, which will be dealt with by the prepareTxn RPC.
    if (this.preparedTxnIdSet.contains(txnId)) {
      Txn txn = this.preparedTxnTable.get(txnId);
      if (txn != null) {
        // The prepare-phase finishes acquiring exclusive locks.
        this.lockTable.releaseMultiExclusiveLocks(txnId, txn.writeKeyList);
      }
      // The prepare-phase may not finish yet, which will be dealt with by the prepareTxn RPC.
      // Shared locks are released at the prepare phase
      this.preparedTxnIdSet.remove(txnId);
      this.txnLog.logAbortedTxn(txnId);
    }
    
    this.preparedTxnTable.remove(txnId);
    this.nonPreparedTxnTable.remove(txnId);// May not be PREPARED
        
    return true;
  }

  /**
   * Commit-phase rules in the ReplicatedCommit protocol as per the paper:
   * 
   * 1. Performs updates
   * 
   * 2. Logs the commit operation in to the TxnLog
   * 
   * 3. Releases locks
   * 
   * @param txnId
   * @return True if the server can commit the txn. Otherwise, false.
   */
  public Boolean commitTxn(String txnId) {
    // Marks the txn as to be committed by the coordinator, but not actually committed.
    this.toCommitTxnSet.add(txnId); 

    //if ((!this.preparedTxnIdSet.contains(txnId)) && (!this.nonPreparedTxnTable.containsKey(txnId))) {
    /*
     * Should check on the preparedTxTable instead of the preparedTxnIdSet because
     * the prepare-phase is done when the txn info is recorded in the table, and we
     * need to retrive the txn info back at the commit-phase.
     */
    if ((!this.preparedTxnTable.containsKey(txnId)) && (!this.nonPreparedTxnTable.containsKey(txnId))) {
      // This server does not finish performing the prepare phase yet,
      // but the txn is committed because of Paxos consensus.
      return true;
    }
    
    return this.doCommitTxn(txnId);
  }
  
  private Boolean doCommitTxn(String txnId) {
    // Makes sure that a txn only commits once
    Boolean isNotCommitted = this.toCommitTxnSet.remove(txnId);
    if (isNotCommitted == false) {
      // The txn has been actually committed.
      return true;
    }
        
    boolean isPrepared = true;
    Txn txn = this.preparedTxnTable.get(txnId);
    if (txn == null) {
      isPrepared = false;
      // Not PREPARED on this server, but the txn coordinator commits the txn because of Paxos consensus
      txn = this.nonPreparedTxnTable.get(txnId);
      if (txn == null) {
        logger.error("Can not commit an unseen txn, txnId = " + txnId + ". Debug!");
        return false;
      }
    }
    
    if (logger.isDebugEnabled()) {
      logger.debug("Commits txn id = " + txn.txnId +
          ", read-key list = " + Utils.toString(txn.readKeyList) +
          ", write-key list = " + Utils.toString(txn.writeKeyList));
    }
    
    if (txn.writeKeyList != null && txn.writeKeyList.length != 0) {
      // Performs updates to data storage
      for (int i = 0; i < txn.writeKeyList.length; i++) {
        this.keyValStore.write(txn.writeKeyList[i], txn.writeValList[i]);
      }
    }
    // Logs the commit operations
    this.txnLog.logCommittedTxn(txnId, txn.writeKeyList, txn.writeValList);
    // Releases exclusive locks. Shared locks are released at prepare phase
    this.lockTable.releaseMultiExclusiveLocks(txnId, txn.writeKeyList);
    
    // Cleans up any soft state abort prepared txns
    this.preparedTxnIdSet.remove(txnId);
    if (isPrepared) {
      this.preparedTxnTable.remove(txnId);
    } else {
      this.nonPreparedTxnTable.remove(txnId);
    }
    
    return true;
  }
  
  // Helper methods
  /**
   * Directly inserts a data. This is a non-txn operation.
   * @param key
   * @param val
   * @return
   */
  public Boolean insertData(String key, String val) {
    return this.keyValStore.write(key, val);
  }
  
  /**
   * Load data from the given data file.
   * 
   * Data File Format
   * key1=value1
   * key2=value2
   * ...
   *
   * @param dataFile
   * @return true if successful, otherwise false.
   */
  public Boolean loadData(String dataFile) {
    return this.keyValStore.loadData(dataFile);
  }
  
  /**
   * Dump data into the given data file directory or data file, or just sync data
   * to hard drive if the backend key-value store supports.
   * 
   * @param dataFile
   * @return true if successful, otherwise false
   */
  public Boolean dumpOrSyncData(String dataFileDir, String dataFile) {
    return this.keyValStore.dumpData(dataFileDir, dataFile);
  }
  
  /**
   * Flushes in-memory data to hard drive if any
   */
  public Boolean sync() {
    this.keyValStore.sync();
    return true;
  }
}
