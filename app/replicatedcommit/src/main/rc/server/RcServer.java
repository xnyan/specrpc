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

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rc.common.RcServerLocationService;
import rc.common.RcConstants;
import rc.common.RcConstants.RPC_FRAMEWORK;
import rc.server.db.RcDatabase;
import rc.server.db.storage.KeyValStore;
import rc.server.db.txn.RcTxnCoordinator;
import rc.server.grpc.RcGrpcServer;
import rc.server.specrpc.RcServiceSpecRpcFactory;
import rc.server.tradrpc.RcServiceTradRpcFactory;
import rpc.execption.UninitializationException;
import rpc.server.api.RpcHostObjectFactory;
import rpc.server.api.RpcServer;
import specrpc.client.api.SpecRpcClient;
import specrpc.common.RpcSignature;
import specrpc.server.api.SpecRpcServer;
import tradrpc.client.api.TradRpcClient;
import tradrpc.server.api.TradRpcServer;

public class RcServer implements Runnable {
  
  private static final Logger logger = LoggerFactory.getLogger(RcConstants.LOGGER_TYPE);
  
  // Database instance.
  public static RcDatabase RC_DB = null;
  // The mappings from keys to server IDs
  public static RcServerLocationService RC_SERVER_LOCATION_SERVICE = null;
  public static String DC_ID = null;
  public static String SHARD_ID = null;
  public static String SERVER_ID = null;
  private static boolean isInit = false;
  
  // Txn coordinator
  public static RcTxnCoordinator RC_TXN_COORDINATOR = null;
 
  public static synchronized void initDatabase (Properties serverConfig) {
    if (RC_DB != null) {
      logger.warn("Database has been initialized. Can not initialize it again.");
      return;
    }
    RC_DB = new RcDatabase(serverConfig);
  }
  
  public static synchronized void initServerLocationService (Properties serverConfig) {
    if (RC_SERVER_LOCATION_SERVICE != null) {
      logger.warn("The key to server mapping has been initialized. Can not initialize it again.");
      return;
    }
    RC_SERVER_LOCATION_SERVICE = new RcServerLocationService(serverConfig);
  }
  
  public static synchronized void initTxnCoordinator(Properties serverConfig, RPC_FRAMEWORK rpcFramework) {
    if (RC_TXN_COORDINATOR != null ) {
      logger.warn("Txn coordinator module has been initialized. Can not initialize it again.");
      return;
    }
    RC_TXN_COORDINATOR = new RcTxnCoordinator(serverConfig, rpcFramework);
  }
  
  public static synchronized void initServerProperties(Properties serverConfig) {
    if (isInit == true) {
      logger.warn("RcServer properties have been initialized. Cnan not initialize them again");
      return;
    }
    DC_ID = serverConfig.getProperty(RcConstants.DC_ID_PROPERTY);
    SHARD_ID = serverConfig.getProperty(RcConstants.DC_SHARD_ID_PROPERTY);
    SERVER_ID = serverConfig.getProperty(RcConstants.SERVER_ID_PROPERTY);
  }
  
  public final String dcId;
  public final String shardId;
  public final String serverId;
  public final String ip;
  public final int port;
  public RPC_FRAMEWORK rpcFramework;
  private RpcServer rpcServer; // for SpecRPC and TradRPC
  private RcGrpcServer grpcServer; // for Grpc

  public RcServer(String dcId, String shardId, String ip, int port, Properties serverConfig) {
    this.dcId = dcId;
    this.shardId = shardId;
    this.serverId = this.dcId + RcConstants.SERVER_ID_REGEX + this.shardId;
    
    serverConfig.setProperty(RcConstants.DC_ID_PROPERTY, this.dcId);
    serverConfig.setProperty(RcConstants.DC_SHARD_ID_PROPERTY, this.shardId);
    serverConfig.setProperty(RcConstants.SERVER_ID_PROPERTY, this.serverId);
    
    this.ip = ip;
    this.port = port;
    
    // Initializes the key to server mapping
    initServerLocationService(serverConfig);
          
    initServerProperties(serverConfig);
    
    // Initializes database
    initDatabase(serverConfig);
    
    // Initializes RPC framework
    String rpcConfigFile = serverConfig.getProperty(RcConstants.RPC_CONFIG_FILE_PROPERTY);
    String rpcFrameworkType = serverConfig.getProperty(
        RcConstants.RPC_FRAMEWORK_PROPERTY, 
        RcConstants.DEFAULT_RPC_FRAMEWORK);
    this.rpcFramework = RPC_FRAMEWORK.valueOf(rpcFrameworkType.toUpperCase());

    RpcHostObjectFactory rpcHostObjectFactory = null;
    
    try {
      switch (this.rpcFramework) {
      case TRADRPC:
        this.rpcServer = new TradRpcServer();
        rpcHostObjectFactory = new RcServiceTradRpcFactory();
        this.initRpcServerFramework(rpcHostObjectFactory, rpcConfigFile);
        // Initializes client-side framework
        TradRpcClient.initClient(rpcConfigFile); 
        break;
      case SPECRPC:
        this.rpcServer = new SpecRpcServer();
        rpcHostObjectFactory = new RcServiceSpecRpcFactory();
        this.initRpcServerFramework(rpcHostObjectFactory, rpcConfigFile);
        // Initializes client-side framework
        SpecRpcClient.initClient(rpcConfigFile);
        break;
      case GRPC:
        // Initializes GRPC framework
        this.grpcServer = new RcGrpcServer(this.port);
        break;
      default:
        logger.error("Undefined RPC framework: " + this.rpcFramework);
        System.exit(RcConstants.INIT_FAIL_ERROR_CODE);
      }
    } catch (IOException e) {
      logger.error(e.getMessage());
      e.printStackTrace();
      System.exit(RcConstants.INIT_FAIL_ERROR_CODE);
    }
    
    initTxnCoordinator(serverConfig, this.rpcFramework);
  }
  
  private void initRpcServerFramework(RpcHostObjectFactory rpcHostObjectFactory, String rpcConfigFile) {
    try {
      // Initializes RPC server-side framework
      this.rpcServer.initServer(this.serverId, this.ip, this.port, rpcConfigFile);
      // RPCs to be registered
      RpcSignature[] rpcSigList = {
          RcService.RPC_READ, // RPCs provided by non-coordinators, for txn clients to call
          RcService.RPC_CLIENT_ABORT,
          RcService.RPC_PREPARE_TXN,  // RPCs provided by non-coordinators, for txn coordinators to call
          RcService.RPC_ABORT_TXN,
          RcService.RPC_COMMIT_TXN,
          RcService.RPC_PROPOSE_TO_COMMIT_TXN,  // RPCs provided by coordinators, for txn clients to call
          RcService.RPC_VOTE_TO_COMMIT_TXN,  // RPCs provided by coordinators, for other txn coordinators to call
          // For SpecRPC only
          RcService.RPC_PREPARE_TXN_AFTER_SPEC,
          // Database helper RPCs
          RcService.RPC_PARSE_AND_LOAD_DATA,
          RcService.RPC_DUMP_OR_SYNC_DATA
      };
      // Registers RPCs for clients to call
      for (RpcSignature rpcSig : rpcSigList) {
        this.rpcServer.register(rpcSig.methodName, rpcHostObjectFactory, rpcSig.returnType, rpcSig.argTypes);
      }
      
    } catch (IOException | UninitializationException e) {
      logger.error(e.getMessage());
      e.printStackTrace();
      System.exit(RcConstants.INIT_FAIL_ERROR_CODE);
    }
  }

  @Override
  public void run() {
    // Starts up the RPC framework and waits for clients to call RPCs
    try {
      switch(this.rpcFramework) {
      case TRADRPC:
      case SPECRPC:
        this.rpcServer.execute();
        break;
      case GRPC:
        // Starts GRPC framework
        this.grpcServer.execute();
        break;
      default:
        logger.error("Undefined RPC framework: " + this.rpcFramework);  
      }
    } catch (UninitializationException | InterruptedException | IOException e) {
      logger.error(e.getMessage());
    }
  }
  
  // Database helper methods
  
  /**
   * Only load the data that are mapped to this shard
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
  public static Boolean parseAndLoadData(String dataFile) {
    if (RcServer.RC_DB == null) {
      logger.error("Database is not initialized. Initialize an RcServer instance first.");
      System.exit(RcConstants.RUNTIME_FATAL_ERROR_CODE);
    }
    
    BufferedReader reader = null;
    try {
      reader = new BufferedReader(new FileReader(dataFile));
    } catch (FileNotFoundException e) {
      logger.error("Data file does not exist. File: " + dataFile);
      logger.error(e.getMessage());
      return false;
    }
    
    String line = null;
    try {
      while ((line = reader.readLine()) != null) {
        line = line.trim();
        int regexIndex = line.indexOf(KeyValStore.KEY_VAL_REGEX);
        String key = line.substring(0, regexIndex);
        if (RcServer.RC_SERVER_LOCATION_SERVICE.getShardId(key).equals(SHARD_ID)) {
          String val = line.substring(regexIndex + 1, line.length());
          RcServer.RC_DB.insertData(key, val);
        }
      }
      reader.close();
    } catch (IOException e) {
      logger.error(e.getMessage());
      return false;
    }

    return true;
  }
}
