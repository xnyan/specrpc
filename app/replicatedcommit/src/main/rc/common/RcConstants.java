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

import rc.client.txn.ClientTxnOperation;
import rc.server.db.log.AsyncTxnLogger;
import rc.server.db.storage.InMemKeyValStore;

public class RcConstants {
  /**
   * Constant numbers
   */
  public static final int PERCENTAGE = 100;
  
  /**
   * Helper data structures
   */
  // Helps to convert object[] to string[]
  public static final String[] STRING_ARRAY_TYPE_HELPER = new String[0];
  // Helps to convert object[] to ClientTxnOperation[]
  public static final ClientTxnOperation[] CLIENT_TXN_OPERATION_ARRAY_TYPE_HELPER = new ClientTxnOperation[0];
  // Reads null result
  public static final String READ_NULL_VALUE = "NULL"; // This is a string substitution for the non-exist data value: null
  public static final long INVALID_DATA_VERSION = -1;

  /**
   * Error code
   */
  public static final int INIT_FAIL_ERROR_CODE = -1;
  public static final int RUNTIME_FATAL_ERROR_CODE = -2;

  /**
   * Log configuration
   */
  public static final String LOGGER_TYPE_CONF = "logger.type";
  public static final String DEFAULT_LOGGER_TYPE = "Console";
  public static final String LOGGER_TYPE = System.getProperty(LOGGER_TYPE_CONF, DEFAULT_LOGGER_TYPE);

  /**
   * RPC Framework Configurations
   */
  public enum RPC_FRAMEWORK {
    TRADRPC, SPECRPC, GRPC
  };

  public static final String RPC_FRAMEWORK_PROPERTY = "rc.rpc.framework";
  public static final String DEFAULT_RPC_FRAMEWORK = RPC_FRAMEWORK.TRADRPC + "";

  public static final String RPC_CONFIG_FILE_PROPERTY = "rc.rpc.config.file";
  
  public static final String RPC_SPECRPC_READ_PROXY_PORT_PROPERTY = "rc.rpc.specrpc.readProxy.port";
  public static final String DEFAULT_RPC_SPECRPC_READ_PROXY_PORT = "3000";
  public static final String RPC_SPECRPC_STATISTICS_ENABLED_PROPERTY = "rc.rpc.specrpc.statistics.enabled";
  public static final String DEFAULT_RPC_SPECRPC_STATISTICS_ENABLED = "true";
  
  /**
   * Data Configurations
   */
  public static final String DATA_KEY_FILE_PROPERTY= "rc.data.keyfile";
  
  /**
   * Client Lib Configurations
   */
  public static final String CLIENT_LIB_ID_PROPERTY = "rc.client.lib.id";
  public static final String CLIENT_LIB_THREAD_POOL_SIZE_PROPERTY = "rc.client.lib.threadPool.size";
  public static final String DEFAULT_CLIENT_LIB_THREAD_POOL_SIZE = "0";
  public static final String CLIENT_LIB_SPECRPC_READ_PROXY_START_TIME_PROPERTY = "rc.client.lib.specrpc.readproxy.starttime";
  public static final String DEFAULT_CLIENT_LIB_SPECRPC_READ_PROXY_START_TIME = "500"; //ms
  
  /**
   * Benchmark / Workload Configurations
   */
  public enum DATA_DISTRIBUTION {
    ZIPF, UNIFORM
  };
  
  public enum WORKLOAD_TYPE {
    YCSBT, RETWIS
  };
  
  // Benchmark running time in seconds. Default is 60 seconds 
  // e.g. PT5h, PT5m, or PT5s means 5 hours, minutes or seconds.
  public static final String BENCHMARK_RUNNING_TIME_PROPERTY = "rc.benchmark.running.time";
  public static final String DEFAULT_BENCHMARK_RUNNING_TIME = "PT60s";
  // Target Throughput (#Txn/s), default is 0, which means sending txns back-to-back, which is the max.
  public static final String BENCHMARK_TARGET_THROUGHPUT_PROPERTY = "rc.benchmark.throughput";
  public static final String DEFAULT_BENCHMARK_TARGET_THROUGHPUT = "0";
  
  // Random seed: 0 for dynamic (default)
  // NOTE: When there are multiple clients sharing the same configuration file, 
  // client code should guarantee that the random seeds for all clients are different. 
  // Otherwise, the clients will use the same random seed to generate the same workload.
  public static final String WORKLOAD_RANDOM_SEED_PROPERTY = "rc.workload.random.seed";
  public static final String DEFAULT_WORKLOAD_RANDOM_SEED = "0";
  // Key distribution: zipf or uniform. Zipf with alpha = 0 equals to uniform
  public static final String WORKLOAD_KEY_DISTRIBUTION_PROPERTY = "rc.workload.keydistribution";
  public static final String DEFAULT_WORKLOAD_KEY_DISTRIBUTION = "zipf";
  // Zipf alpha value
  public static final String WORKLOAD_ZIPF_ALPHA_PROPERTY = "rc.workload.zipf.alpha";
  public static final String DEFAULT_WORKLOAD_ZIPF_ALPHA = "0.75";
  // Workload type: ycsbt or retwis
  public static final String WORKLOAD_TYPE_PROPERTY = "rc.workload";
  public static final String DEFAULT_WORKLOAD_TYPE = WORKLOAD_TYPE.YCSBT.toString();
  
  // YCSB+T Configurations
  // YCSB+T: the number of operations per txn
  public static final String WORKLOAD_YCSBT_OP_NUM_PROPERTY = "rc.workload.ycsbt.operation.num";
  public static final String DEFAULT_WORKLOAD_YCSBT_OP_NUM = "4";
  // YCSB+T: the percentage of write operations per txn
  public static final String WORKLOAD_YCSBT_WRITE_PORTION_PROPERTY = "rc.workload.ycsbt.writeportion";
  public static final String DEFAULT_WORKLOAD_YCSBT_WRITE_PORTION = "50";
  // YCSB+T: the percentage of read operations per txn
  public static final String WORKLOAD_YCSBT_READ_PORTION_PROPERTY = "rc.workload.ycsbt.readportion";
  public static final String DEFAULT_WORKLOAD_YCSBT_READ_PORTION = "50";
  // YCSB+T: the percentage of rmw operations per txn
  // A rmw operation counts one operation but consists of one read and one write on the same key.
  public static final String WORKLOAD_YCSBT_RMW_PORTION_PROPERTY = "rc.workload.ycsbt.rmwportion";
  public static final String DEFAULT_WORKLOAD_YCSBT_RMW_PORTION = "0";
  
  // Retwis Configurations
  // Retwis: the percentage of add-user operation per txn
  public static final String WORKLOAD_RETWIS_ADDUSER_PORTION_PROPERTY = "rc.workload.retwis.adduser";
  public static final String DEFAULT_WORKLOAD_RETWIS_ADDUSER_PORTION = "5";
  // Retwis: the percentage of follow/unfollow operation per txn
  public static final String WORKLOAD_RETWIS_FOLLOW_PORTION_PROPERTY = "rc.workload.retwis.follow";
  public static final String DEFAULT_WORKLOAD_RETWIS_FOLLOW_PORTION = "15";
  // Retwis: the percentage of post-tweet operation per txn
  public static final String WORKLOAD_RETWIS_POST_PORTION_PROPERTY = "rc.workload.retwis.post";
  public static final String DEFAULT_WORKLOAD_RETWIS_POST_PORTION = "30";
  // Retwis: the percentage of load-timeline operation per txn
  public static final String WORKLOAD_RETWIS_LOAD_PORTION_PROPERTY = "rc.workload.retwis.load";
  public static final String DEFAULT_WORKLOAD_RETWIS_LOAD_PORTION = "50";
  
  /**
   * Server Configurations
   */
  public static final String DC_ID_PROPERTY = "rc.dc.id";
  public static final String DC_SHARD_ID_PROPERTY = "rc.dc.shard.id";
  public static final String SERVER_ID_PROPERTY = "rc.server.id";
  /**
   * Txn coordinator configuration.
   */
  // Thread pool size
  public static final String TXN_COORD_THREAD_POOL_SIZE_PROPERTY = "rc.txn.coordinator.threadPool.size";
  public static final String DEFAULT_TXN_COORD_THREAD_POOL_SIZE = "0";

  /**
   * Database configuration
   */

  /**
   * Database backend storage configuration
   */
  public static final String DB_STORAGE_CLASS_PROPERTY = "rc.db.storage.class";
  public static final String DEFAULT_DB_STORAGE_CLASS = InMemKeyValStore.class.getName();

  // BerkeleyDB configuration
  // BerkeleyDB environment directory
  public static final String DB_STORAGE_BDB_ENV_HOME_PROPERTY = "rc.db.storage.bdb.env.dir";
  public static final String DB_STORAGE_BDB_ENV_HOME_PREFIX = "bdb-";
  // If BerkeleyDB is readOnly
  public static final String DB_STORAGE_BDB_READ_ONLY_PROPERTY = "rc.db.storage.bdb.readOnly";
  public static final String DEFAULT_DB_STORAGE_BDB_READ_ONLY = "false";

  // If BerkeleyDB supports transactions
  public static final String DB_STORAGE_BDB_TXN_PROPERTY = "rc.db.storage.bdb.txn";
  public static final String DEFAULT_DB_STORAGE_BDB_TXN = "false";
  // BerkeleyDB lock timeout in both transaction and non-transaction model.
  // e.g. PT5h, PT5m, or PT5s means 5 hours, minutes or seconds.
  public static final String DB_STORAGE_BDB_LOCK_TIMEOUT_PROPERTY = "rc.db.storage.bdb.lockTimeout";
  public static final String DEFAULT_DB_STORAGE_BDB_LOCK_TIMEOUT = "PT5m";
  // BerkeleyDB cache size, i.e. memory pool, which must be at least the size of
  // working set pluses some extra size
  // e.g. 2gb, 2m, 2kilobytes, or 4B means 2GB, 2MB, 2KB or 4B
  // Note: BerkeleyDB requires min 20KB, max 4GB or 10TB on 32-bit or 64-bit OS,
  // with default as 256KB
  public static final String DB_STORAGE_BDB_CACHE_SIZE_PROPERTY = "rc.db.storage.bdb.cacheSize";
  public static final String DEFAULT_DB_STORAGE_BDB_CACHE_SIZE = "1g";

  /**
   * Database txn log configuration
   */
  public static final String DB_TXN_LOG_CLASS_PROPERTY = "rc.db.txn.log.class";
  public static final String DEFAULT_DB_TXN_LOG_CLASS = AsyncTxnLogger.class.getName();

  // Log file
  public static final String DB_TXN_LOG_FILE_PROPERTY = "rc.db.txn.log.file";
  // If log file is not defined, use the following directory, prefix, and suffix
  // to construct one.
  // Log file path and name: directory/prefix + serverId + suffix
  public static final String DB_TXN_LOG_DIR_PROPERTY = "rc.db.txn.log.dir";
  public static final String DEFAULT_DB_TXN_LOG_DIR = "./";
  public static final String DB_TXN_LOG_FILE_PREFIX_PROPERTY = "rc.db.txn.log.file.prefix";
  public static final String DEFAULT_DB_TXN_LOG_FILE_PREFIX = "server-";
  public static final String DB_TXN_LOG_FILE_SUFFIX_PROPERTY = "rc.db.txn.log.file.suffix";
  public static final String DEFAULT_DB_TXN_LOG_FILE_SUFFIX = "-txn.log";

  // If appends to existing log files. If false, create a new log file.
  public static final String DB_TXN_LOG_FILE_APPEND_PROPERTY = "rc.db.txn.log.file.append";
  public static final String DEFAULT_DB_TXN_LOG_FILE_APPEND = "false";

  /**
   * Data centers and shards/servers configuration
   */
  // Number of DCs
  public static final String DC_NUM_PROPERTY = "rc.dc.num";
  public static final String DC_ID_LIST_PROPERTY = "rc.dc.id.list"; // Format: 1,2,3,4
  // Number of shards. Each DC has identical shards in ReplicatedCommit.
  public static final String DC_SHARD_NUM_PROPERTY = "rc.dc.shard.num";
  public static final String DC_SHARD_ID_LIST_PROPERTY = "rc.dc.shard.id.list"; // Format: 1,2,3,4,5
  public static final String ID_REGEX = ",";
  public static final String SERVER_ID_REGEX = "-"; // Server id is "dcId" + SERVER_ID_REGEX + "shardId"
  
}
