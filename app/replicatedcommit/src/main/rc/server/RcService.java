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

package rc.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rc.common.RcConstants;
import rc.common.TxnReadResult;
import rc.common.Utils;
import specrpc.common.RpcSignature;

public abstract class RcService {
  
  private static final Logger logger = LoggerFactory.getLogger(RcConstants.LOGGER_TYPE);
  
  // RPCs provided by non-coordinators, for txn clients to call
  public static final String RPC_READ_NAME = "read";
  public static final RpcSignature RPC_READ = new RpcSignature(
      RcService.class.getName(), // Class name
      RPC_READ_NAME, // Method name
      TxnReadResult.class, // Return type
      String.class, // txn id
      String.class  // read key
      ); 
  /**
   * If acquiring read lock successfully, returns read value. Otherwise, returns
   * null as the read value and marks lock not acquired.
   * 
   * @param txnId
   * @param readKey
   * @return readVal
   */
  public TxnReadResult read(String txnId, String readKey) {
    TxnReadResult res = RcServer.RC_DB.read(txnId, readKey);
    
    if (logger.isDebugEnabled()) {
      logger.debug("Read txn id = " + txnId + 
          ", key = " + readKey + 
          ", result = " + res.toString());
    }
    
    return res;
  }
  
  public static final String RPC_CLIENT_ABORT_NAME = "clientAbortTxn";
  public static final RpcSignature RPC_CLIENT_ABORT = new RpcSignature(
      RcService.class.getName(), // Class name
      RPC_CLIENT_ABORT_NAME,
      Boolean.class, // Return type
      String.class, // txn id
      String[].class // read-key list
      );
  /**
   * Aborts a txn when a client calls to abort the txn. This can only happen
   * before the client calls commit, which makes a transaction to be prepared.
   * 
   * @param txnId
   * @param readKeyList
   * @return True if aborting the txn successfully.
   */
  public Boolean clientAbortTxn(String txnId, String[] readKeyList) {
    Boolean isAborted = RcServer.RC_DB.clientAbortTxn(txnId, readKeyList);
    
    if (logger.isDebugEnabled()) {
      logger.debug("Client aborts txn id = "  + txnId + 
          ", isAborted = " + isAborted +
          ", read-key list = " + Utils.toString(readKeyList));
    }
    
    return isAborted;
  }
  
  
  // RPCs provided by non-coordinators, for txn coordinators to call
  public static final String RPC_PREAPRE_TXN_NAME = "prepareTxn";
  public static final RpcSignature RPC_PREPARE_TXN = new RpcSignature(
      RcService.class.getName(), // Class name
      RPC_PREAPRE_TXN_NAME, // Method name
      Boolean.class, // Return type
      String.class, // txn Id
      String[].class, // read key list
      String[].class, // write key list
      String[].class // write value list
      );
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
    Boolean isPrepared = RcServer.RC_DB.prepareTxn(txnId, readKeyList, writeKeyList, writeValList);
    
    if (logger.isDebugEnabled()) {
      logger.debug("Prepare txn id = "  + txnId + 
          ", isPrepared = " + isPrepared +
          ", read-key list = " + Utils.toString(readKeyList) +
          ", write-key list = " + Utils.toString(writeKeyList));
    }
    
    return isPrepared;
  }
  
  public static final String RPC_ABORT_TXN_NAME = "abortTxn";
  public static final RpcSignature RPC_ABORT_TXN = new RpcSignature(
      RcService.class.getName(), // Class name
      RPC_ABORT_TXN_NAME, // Method name
      Boolean.class, // Return type
      String.class  // txn Id
      );
  /**
   * Aborts a txn when the coordinator calls to abort the txn. The txn may or may
   * not be PREPARED yet.
   * 
   * @param txnId
   * @return True if aborting the txn successfully
   */
  public Boolean abortTxn(String txnId) {
    Boolean isAborted = RcServer.RC_DB.abortTxn(txnId);
    
    if (logger.isDebugEnabled()) {
      logger.debug("Aborts txn id = " + txnId +
          ", isAborted = " + isAborted);
    }
    
    return isAborted;
  }
  
  public static final String RPC_COMMIT_TXN_NAME =  "commitTxn";
  public static final RpcSignature RPC_COMMIT_TXN = new RpcSignature(
      RcService.class.getName(), // Class name
      RPC_COMMIT_TXN_NAME,  // Method name
      Boolean.class,  // Return type
      String.class  // txn Id
      );
  /**
   * Commit-phase rules in the ReplicatedCOmmit protocol as per the paper:
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
    Boolean isCommitted = RcServer.RC_DB.commitTxn(txnId);
    
    if (logger.isDebugEnabled()) {
      logger.debug("Commits txn id = " + txnId +
          ", isCommitted = " + isCommitted);
    }
    
    return isCommitted;
  }
  
  
  // RPCs provided by coordinators, for txn clients to call
  public static final String RPC_PROPOSE_TO_COMMIT_TXN_NAME = "proposeToCommitTxn";
  public static final RpcSignature RPC_PROPOSE_TO_COMMIT_TXN = new RpcSignature(
      RcService.class.getName(),  // Class name
      RPC_PROPOSE_TO_COMMIT_TXN_NAME, // Method name
      Boolean.class,  // Return type
      String.class, // txn Id
      String[].class, // read key list
      String[].class, // write key list
      String[].class  // write value list
      );
  /**
   * Coordinator role
   * 
   * Starts 2PC prepare phase by invoking RPC prepareTxn() to make participant
   * servers prepare for the txn.
   * 
   * If there is one server aborting the txn, the prepare-phase result is false.
   * We DO NOT call abort to every participant server at this point, since the txn
   * may become committed after Paxos consensus.
   * 
   * If all servers prepare to commit the txn, the prepare-phase result is true.
   * 
   * Asynchronously broadcasts the prepare-phase result to all coordinators in all
   * DCs by using RPC voteToCommitTxn(), and returns the result.
   *
   * @param txnId
   * @param readKeyList
   * @param writeKeyList
   * @param writeValList
   * @return True if 2PC prepare-phase result is true. Otherwise, false.
   */
  public Boolean proposeToCommitTxn(String txnId, String[] readKeyList, String[] writeKeyList, String[] writeValList) {
    Boolean isToCommit = RcServer.RC_TXN_COORDINATOR.proposeToCommitTxn(txnId, readKeyList, writeKeyList, writeValList);
    
    if (logger.isDebugEnabled()) {
      logger.debug("Proposes to commit txn id = " + txnId + 
          ", isToCommit = " + isToCommit +
          ", read-key list = " + Utils.toString(readKeyList) +
          ", write-key list = " + Utils.toString(writeKeyList));
    }
    
    return isToCommit;
  }
  
  
  // RPCs provided by coordinators, for other txn coordinators to call
  public static final String RPC_VOTE_TO_COMMIT_TXN_NAME = "voteToCommitTxn";
  public static final RpcSignature RPC_VOTE_TO_COMMIT_TXN = new RpcSignature(
      RcService.class.getName(),  // Class name
      RPC_VOTE_TO_COMMIT_TXN_NAME,  // Method name
      Boolean.class,  // Return type
      String.class, // txn Id
      Boolean.class // is voting to commit
      );
  /**
   * Processes a vote abort if to commit the specified txn.
   * 
   * If there is a quorum of vote to commit the txn, asynchronously notifies
   * participant servers in the same DC to commit the txn.
   * 
   * If there is quorum of vote NOT to commit the txn, aysnchronously notifies
   * participant servers in the same DC to abort the txn.
   * 
   * Otherwise, do nothing.
   * 
   * @param txnId
   * @param isVoteToCommit
   * @return False if consensus has NOT been made. Otherwise, true.
   */
  public Boolean voteToCommitTxn(String txnId, Boolean isVoteToCommit) {
    Boolean isConsensusMade = RcServer.RC_TXN_COORDINATOR.voteToCommitTxn(txnId, isVoteToCommit);
    
    if (logger.isDebugEnabled()) {
      logger.debug("Votes to commit txn id = " + txnId +
          ", isVoteToCommit = " + isVoteToCommit + 
          ", isConsensusMade = " + isConsensusMade);
    }
    
    return isConsensusMade;
  }
 
  // RPCs required only for Replicated Commit Protocol with SpecRPC
  
  //Deprecated (NOT necessary / used in ReplicatedCommit)
  public static final String RPC_PREPARE_TXN_AFTER_SPEC_NAME = "prepareTxnAfterSpec";
  public static final RpcSignature RPC_PREPARE_TXN_AFTER_SPEC = new RpcSignature(
      RcService.class.getName(), // Class name
      RPC_PREPARE_TXN_AFTER_SPEC_NAME, // Method name
      Boolean.class, // Return type
      String.class, // txn Id
      String[].class, // read key list
      String[].class, // write key list
      String[].class, // write value list
      String[].class  // non-required read key list
      );
  /**
   * This is NOT necessary/used in ReplicatedCommit because 
   * exclusive-lock acquiring will takes over any acquired shared locks.
   * 
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
  public Boolean prepareTxnAfterSpec(
      String txnId,
      String[] readKeyList,
      String[] writeKeyList,
      String[] writeValList,
      String[] nonRequiredReadKeyList) {
    Boolean isPrepared = RcServer.RC_DB.prepareTxnAfterSpec(
        txnId,
        readKeyList,
        writeKeyList,
        writeValList,
        nonRequiredReadKeyList);
    
    if (logger.isDebugEnabled()) {
      logger.debug("Prepare txn id = "  + txnId + 
          ", isPrepared = " + isPrepared +
          ", read-key list = " + Utils.toString(readKeyList) +
          ", write-key list = " + Utils.toString(writeKeyList) +
          ", non-required read-key list = " + Utils.toString(nonRequiredReadKeyList));
    }
    
    return isPrepared;
  }
  

  // Database helper RPCs
  
  public static final String RPC_PARSE_AND_LOAD_DATA_NAME = "parseAndLoadData";
  public static final RpcSignature RPC_PARSE_AND_LOAD_DATA = new RpcSignature(
      RcService.class.getName(), // Class name
      RPC_PARSE_AND_LOAD_DATA_NAME, // Method name
      Boolean.class, // Return type
      String.class // data file
      );
  /**
   * Only loads the data that are mapped to this shard
   * from the given data file.
   * 
   * Data File Format
   * key1=value1
   * key2=value2
   * ...
   *
   * @param dataFile
   * @return true if successful, otherwise false.
   */
  public Boolean parseAndLoadData(String dataFile) {
    Boolean res = RcServer.parseAndLoadData(dataFile);
    
    if (logger.isDebugEnabled()) {
      logger.debug("Parses and loads data from " + dataFile +
          ", loading status = " + (res ? "SUCCEED" : "FAILED"));
    }
    
    return res;
  }
  
  public static final String RPC_DUMP_OR_SYNC_DATA_NAME = "parseAndLoadData";
  public static final RpcSignature RPC_DUMP_OR_SYNC_DATA = new RpcSignature(
      RcService.class.getName(), // Class name
      RPC_DUMP_OR_SYNC_DATA_NAME, // Method name
      Boolean.class, // Return type
      String.class, // data file directory
      String.class // data file
      );
  /**
   * Dump data into the given data file directory or data file, or just sync data
   * to hard drive if the backend key-value store supports.
   * 
   * @param dataFile
   * @return true if successful, otherwise false
   */
  public Boolean dumpOrSyncData(String dataFileDir, String dataFile) {
    Boolean res = RcServer.RC_DB.dumpOrSyncData(dataFileDir, dataFile);
    
    if (logger.isDebugEnabled()) {
      logger.debug("Dumps or syncs data is done, status = " + (res ? "SUCCEED" : "FAILED"));
    }
    
    return res;
  }
}
