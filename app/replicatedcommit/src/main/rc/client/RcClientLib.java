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

import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rc.client.grpc.RcClientRpcFacadeGrpc;
import rc.client.specrpc.RcClientRpcFacadeSpecRpc;
import rc.client.tradrpc.RcClientRpcFacadeTradRpc;
import rc.common.PaxosInstance;
import rc.common.RcConstants;
import rc.common.RcServerLocationService;
import rc.common.ServerLocationTable;
import rc.common.RcConstants.RPC_FRAMEWORK;

public abstract class RcClientLib {

  private static final Logger logger = LoggerFactory.getLogger(RcConstants.LOGGER_TYPE);
  
  public static final String TXN_ID_REGEX = "-";
  
  public static String CLIENT_LIB_ID = null;
  public static RcServerLocationService RC_SERVER_LOCATION_SERVICE = null;
  public static int QUORUM_NUM = 0;
  public static ExecutorService THREAD_POOL = null;
  public static RcClientRpcFacade CLIENT_RPC_FACADE = null;
  
  protected static ServerLocationTable SERVER_LOCATION_TABLE = null;// For grpc only
  
  public static PaxosInstance createPaxosInstance() {
    return new PaxosInstance(QUORUM_NUM);
  }
  
  private static synchronized void initClientLibProperties(Properties config) {
    if (CLIENT_LIB_ID != null) {
      logger.warn("There is only one ID for each client lib lass. Can not assign again.");
      return;
    }
    
    CLIENT_LIB_ID = config.getProperty(RcConstants.CLIENT_LIB_ID_PROPERTY);
    if (CLIENT_LIB_ID == null) {
      logger.error("Missing client id. Initializating client lib failed.");
      System.exit(RcConstants.INIT_FAIL_ERROR_CODE);
    }
  }
  
  private static synchronized void initServerLocationService(Properties config) {
    if (RC_SERVER_LOCATION_SERVICE != null) {
      logger.warn("The key to server mapping has been initialized. Can not initialize it again.");
      return;
    }
    RC_SERVER_LOCATION_SERVICE = new RcServerLocationService(config);
    QUORUM_NUM = RcClientLib.RC_SERVER_LOCATION_SERVICE.dcNum/2 + 1;
  }
  
  private static synchronized void initThreadPool(Properties config) {
    if (THREAD_POOL != null) {
      logger.warn("Client lib's thread pool has been initialized. Can not initialize it again.");
      return;
    }
    int threadPoolSize = 0;
    try {
      threadPoolSize = Integer.parseInt(config.getProperty(
          RcConstants.CLIENT_LIB_THREAD_POOL_SIZE_PROPERTY, 
          RcConstants.DEFAULT_CLIENT_LIB_THREAD_POOL_SIZE));
    } catch (NumberFormatException e) {
      logger.error("Client lib initialziation failed because of invalid thread pool size.");
      logger.error(e.getMessage());
      System.exit(RcConstants.INIT_FAIL_ERROR_CODE);
    }
    THREAD_POOL = threadPoolSize > 0 ? 
        Executors.newFixedThreadPool(threadPoolSize) :
        Executors.newCachedThreadPool();
  }
  
  private static synchronized void initClientRpcFacade(RPC_FRAMEWORK rpcFramework) {
    switch (rpcFramework) {
    case TRADRPC:
      CLIENT_RPC_FACADE = new RcClientRpcFacadeTradRpc();
      break;
    case SPECRPC:
      CLIENT_RPC_FACADE = new RcClientRpcFacadeSpecRpc();
      break;
    case GRPC:
      CLIENT_RPC_FACADE = new RcClientRpcFacadeGrpc(RcClientLib.SERVER_LOCATION_TABLE);
      break;
    default:
      logger.error("Invalid RPC framework: " + rpcFramework);
      System.exit(RcConstants.INIT_FAIL_ERROR_CODE);
    }
  }
  
  protected final String rpcConfigFile;
  protected final RPC_FRAMEWORK rpcFramework;
  protected long txnCount;

  public RcClientLib(Properties clientConfig) {
    
    initClientLibProperties(clientConfig);
    
    this.rpcConfigFile = clientConfig.getProperty(RcConstants.RPC_CONFIG_FILE_PROPERTY);
    String rpcFrameworkType = clientConfig.getProperty(
        RcConstants.RPC_FRAMEWORK_PROPERTY, 
        RcConstants.DEFAULT_RPC_FRAMEWORK);
    this.rpcFramework = RPC_FRAMEWORK.valueOf(rpcFrameworkType.toUpperCase());
    this.txnCount = 0;    
    
    initServerLocationService(clientConfig);
    initThreadPool(clientConfig);
    // Initializes RPC framework
    this.initRpcFramework(clientConfig);
    initClientRpcFacade(this.rpcFramework);
  }

  private synchronized long increaseTxnCount() {
    return this.txnCount++;
  }

  public String createTxnId() {
    String txnId = CLIENT_LIB_ID + TXN_ID_REGEX + this.increaseTxnCount();
    return txnId;
  }
  
  protected abstract void initRpcFramework(Properties config);

  /**
   * Starts a transaction
   * 
   * @param CLIENT_LIB_ID
   * @return a transaction object
   */
  public RcClientTxn beginTxn() {
    return this.beginTxn(this.createTxnId());
  }

  public abstract RcClientTxn beginTxn(String txnId);
  
  /**
   * Shutdowns client lib
   */
  public abstract void shutdown();

}
