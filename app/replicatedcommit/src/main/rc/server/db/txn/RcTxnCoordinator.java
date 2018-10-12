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

import java.io.IOException;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Properties;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rc.common.PaxosInstance;
import rc.common.RcConstants;
import rc.common.TxnInfo;
import rc.common.RcConstants.RPC_FRAMEWORK;
import rc.common.ServerLocationTable;
import rc.server.RcServer;
import rc.server.grpc.RcTxnCoordinatorRpcFacadeGrpc;
import rc.server.specrpc.RcTxnCoordinatorRpcFacadeSpecRpc;
import rc.server.tradrpc.RcTxnCoordinatorRpcFacadeTradRpc;
import rpc.config.Constants;
import specrpc.common.RpcConfig;

public class RcTxnCoordinator {

  private static final Logger logger = LoggerFactory.getLogger(RcConstants.LOGGER_TYPE);
  
  // Thread pool for parallel jobs
  private ExecutorService threadPool = null;
  // An RPC facade that provides the same RPC interfaces via different RPC frameworks
  private RcTxnCoordinatorRpcFacade rpcFacade  = null;
  // Paxos manager
  private RcTxnPaxosMap txnPaxosMap;
  // A set of txns that are waiting for Paxos consensus. 
  // Mappings from pending txn ID to a list of participant server IDs
  private Hashtable<String, String[]> pendingTxnTable; // Thread safe.
  
  public RcTxnCoordinator(Properties config, RPC_FRAMEWORK rpcFramework) {
    int threadPoolSize = 0;
    try {
      threadPoolSize = Integer.parseInt(config.getProperty(
          RcConstants.TXN_COORD_THREAD_POOL_SIZE_PROPERTY,
          RcConstants.DEFAULT_TXN_COORD_THREAD_POOL_SIZE));
    } catch (NumberFormatException e) {
      logger.error("Initialziation failed because of invalid thread pool size.");
      logger.error(e.getMessage());
      System.exit(RcConstants.INIT_FAIL_ERROR_CODE);
    }
    this.threadPool = threadPoolSize > 0 ? 
        Executors.newFixedThreadPool(threadPoolSize) :
        Executors.newCachedThreadPool();
        
    // Initializes RPC facade
    switch (rpcFramework) {
    case TRADRPC:
      this.rpcFacade = new RcTxnCoordinatorRpcFacadeTradRpc();
      break;
    case SPECRPC:
      this.rpcFacade = new RcTxnCoordinatorRpcFacadeSpecRpc();
      break;
    case GRPC:
      ServerLocationTable serverLocationTable = null;
      try {
        // Reuses SpecRPC's RPC signature file to locate RPC servers' ip addresses and ports
        String rpcConfigFile = config.getProperty(RcConstants.RPC_CONFIG_FILE_PROPERTY);
        RpcConfig rpcConfig = new RpcConfig(rpcConfigFile);
        String rpcSigFile = rpcConfig.get(
            Constants.RPC_HOST_SIGNATURE_FILE_PROPERTY, 
            Constants.DEFAULT_RPC_HOST_SIGNATURE_FILE);
        serverLocationTable = new ServerLocationTable(rpcSigFile);
      } catch (IOException e) {
        logger.error(e.getMessage());
        e.printStackTrace();
        System.exit(RcConstants.INIT_FAIL_ERROR_CODE);
      }
      
      this.rpcFacade = new RcTxnCoordinatorRpcFacadeGrpc(serverLocationTable);
      break;
    default:
      logger.error("Invalid RPC framework: " + rpcFramework);
      System.exit(RcConstants.INIT_FAIL_ERROR_CODE);
    }
    int quorumNum = RcServer.RC_SERVER_LOCATION_SERVICE.dcNum/2 + 1;
    this.txnPaxosMap = new RcTxnPaxosMap(quorumNum);
    this.pendingTxnTable = new Hashtable<String, String[]>();
  }
  
  /**
   * Starts 2PC prepare phase by invoking RPC to make participant servers prepare
   * for the txn.
   * 
   * If there is one server aborting the txn, the prepare-phase result is false.
   * We DO NOT call abort to every participant server at this point, since the txn
   * may become committed after Paxos consensus.
   * 
   * If all servers prepare to commit the txn, the prepare-phase result is true.
   * 
   * Asynchronously broadcasts the prepare-phase result to all coordinators in all
   * DCs, and returns the result.
   *
   * @param txnId
   * @param readKeyList
   * @param writeKeyList
   * @param writeValList
   * @return True if 2PC prepare-phase result is true. Otherwise, false.
   */
  public Boolean proposeToCommitTxn(String txnId, String[] readKeyList, String[] writeKeyList, String[] writeValList) {    
    /*
     * It is possible that the txn has been decided to be committed or aborted via
     * the Paxos consensus. However, the coordinator in a DC will not proceed to
     * perform the consensus result until the coordinator finishes the prepare
     * phase.
     */

    Boolean isVoteToCommit = true;
    HashMap<String, TxnInfo> serverIdTxnInfoMap = RcServer.RC_SERVER_LOCATION_SERVICE
        .mapTxnToShardServers(RcServer.DC_ID, txnId, readKeyList, writeKeyList, writeValList);
    int totalServerNum = serverIdTxnInfoMap.size();
    ArrayBlockingQueue<RcTxnPrepareResult> prepareResultQueue = 
        new ArrayBlockingQueue<RcTxnPrepareResult>(totalServerNum);
    String[] serverIdList = new String[totalServerNum];
    int index = 0;
    // Starts prepare phase.
    // Asynchronously sends prepare request to all participant servers including the
    // current one, i.e. the coordinator
    for (String serverId : serverIdTxnInfoMap.keySet()) {
      serverIdList[index++] = serverId;
      /*
       * An alternative implementation.
       */
      // RcTxnPrepareRunner runner = new RcTxnPrepareRunner(
      //     serverId,
      //     serverIdTxnInfoMap.get(serverId),
      //     prepareResultQueue,
      //     this.rpcFacade);
      // this.threadPool.execute(runner);
      
      this.threadPool.execute(new Runnable() {
        @Override
        public void run() {
          Boolean isPrepared = false;
          TxnInfo txnInfo = serverIdTxnInfoMap.get(serverId);
          if (serverId.equals(RcServer.SERVER_ID)) {
            // Local prepare request
            isPrepared = RcServer.RC_DB.prepareTxn(
                txnInfo.txnId,
                txnInfo.getReadKeyList(), 
                txnInfo.getWriteKeyList(),
                txnInfo.getWriteValList());
          } else { 
            // Remote prepare request
            isPrepared = rpcFacade.prepareTxn(serverId, txnInfo);
          }
          try {
            prepareResultQueue.put(new RcTxnPrepareResult(serverId, isPrepared));
          } catch (InterruptedException e) {
            logger.error(e.getMessage());
            // TODO Solves that this exception causes the consumer to block on the queue
            // without receiving enough result.
          }
        }
      });
    }
    
    /*
     *  Waits for prepare results from participant servers
     *  
     *  If one participant server aborts the txn, return false.
     *  
     *  If all participant servers are prepared to commit the txn, return true.
     */
    boolean isAbort = false;
    for (int i = 0; i < totalServerNum; i++) {
      RcTxnPrepareResult res;
      try {
        res = prepareResultQueue.take();
      } catch (InterruptedException e) {
        logger.error(e.getMessage());
        break;
      }
      if (res.isPrepared) {
        serverIdTxnInfoMap.get(res.serverId).setTxnStatus(TxnInfo.TXN_STATUS.PREPARED);
      } else {
        serverIdTxnInfoMap.get(res.serverId).setTxnStatus(TxnInfo.TXN_STATUS.ABORTED);
        isAbort = true;
        break;
        /*
         * This early abort will cause that a participant may not see the prepare
         * request but sees a commit or abort request because of Paxos consensus.
         */
      }
    }
    
    // Finishes the prepare phase.
    // Saves the txn as a pending one that requires to achieve Paxos consensus with other DCs.
    this.pendingTxnTable.put(txnId, serverIdList);
    
    if (isAbort) {
      isVoteToCommit = false;
    }
    
    // Asynchronously notifies the txn coordinators in all datacenters (including the current one).
    this.broadcastCommitVote(txnId, isVoteToCommit);
        
    return isVoteToCommit; // Result to clients.
  }
  
  /**
   * Asynchronously inputs self's commit vote to the Paxos instance, and
   * asynchronously sends the commit vote to the txn coordinators in other
   * datacenters.
   * 
   * @param txnId
   * @param isVoteToCommit
   */
  private void broadcastCommitVote(String txnId, Boolean isVoteToCommit) {
    this.threadPool.execute(new Runnable() {
      @Override
      public void run() {
        // Retrieves all coordinators' server IDs, which are the identical shards in all
        // DCs.
        String[] coordServerIdList = RcServer.RC_SERVER_LOCATION_SERVICE.getCoordinatorServerIdList(RcServer.SHARD_ID);
        
        // Asynchronously broadcasts the vote to every coordinator in each DC.
        for (String coordServerId : coordServerIdList) {
          threadPool.execute(new Runnable() {
            @Override
            public void run() {
              if (coordServerId.equals(RcServer.SERVER_ID)) {
                // Local vote
                voteToCommitTxn(txnId, isVoteToCommit);
              } else {
                // Remote vote
                rpcFacade.voteToCommitTxn(coordServerId, txnId, isVoteToCommit);
              }
            }
          });
        }
      }
    });
  }
  
  /**
   * Processes a vote for if to commit the specified txn.
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
    PaxosInstance txnPaxosInstance = this.txnPaxosMap.getTxnPaxosInstance(txnId);
    if (isVoteToCommit) {
      txnPaxosInstance.voteAccept();
    } else {
      txnPaxosInstance.voteReject();
    }
    
    return this.tryPaxosConsensus(txnPaxosInstance, txnId);
  }
  
  private Boolean tryPaxosConsensus(PaxosInstance txnPaxosInstance, String txnId) {
    Boolean isAcceptToCommit = txnPaxosInstance.isAccept();
    if (isAcceptToCommit == null) {
      // Consensus has not been made.
      return false;
    }
    
    /*
     * 1. Avoids committing or aborting a txn multiple times because there are more
     * votes than a quorum. The Paxos consensus should only trigger one commit/abort
     * request to participant servers in the same DC.
     * 
     * 2. Prevents the coordinator in this DC from proceeding if it does not finish
     * the prepare phase.
     */
    String[] serverIdList = this.pendingTxnTable.remove(txnId); // Thread safe
    if (serverIdList == null) {
      /*
       * Either more than a quorum of replicas have the same votes, or the coordinator
       * in this DC has not finished the prepare phase.
       * 
       * In the latter case, the coordinator will proceed this Paxos consensus after
       * finishing the prepare phase. We do not need any synchronization on holding
       * the consensus result.
       */
      return true;
    }
    
    if (isAcceptToCommit) {
      // Asynchronously commits the txn in this DC.
      this.doCommitTxn(txnId, serverIdList);
    } else {
      // Asynchronously aborts the txn in this DC.
      this.doAbortTxn(txnId, serverIdList);
      ;
    }

    return true;
  }
  
  /**
   * Asynchronously commits the specified txn in this DC.
   * @param txnId
   */
  private void doCommitTxn(String txnId, String[] serverIdList) {
    for (String serverId : serverIdList) {
      this.threadPool.execute(new Runnable() {
        @Override
        public void run() {
          if (serverId.equals(RcServer.SERVER_ID)) {
            // Local commit
            RcServer.RC_DB.commitTxn(txnId);
          } else {
            // Remote commit
            rpcFacade.commitTxn(serverId, txnId);
          }
        }
      });
    }
  }
  
  /**
   * Asynchronously aborts the specified txn in this DC.
   * @param txnId
   */
  private void doAbortTxn(String txnId, String[] serverIdList) {
    for (String serverId : serverIdList) {
      this.threadPool.execute(new Runnable() {
        @Override
        public void run() {
          if (serverId.equals(RcServer.SERVER_ID)) {
            // Local abort
            RcServer.RC_DB.abortTxn(txnId);
          } else {
            // Remote abort
            rpcFacade.abortTxn(serverId, txnId);
          }
        }
      });
    }
  }

}
