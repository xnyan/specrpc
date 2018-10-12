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

package micro.common;

public class MicroConstants {

  /**
   * Constants
   */
  public static final int PERCENTAGE = 100;
  public static final String INCORRECT_PREDICTION_DATA = "INCORRECT_PREDICTION_DATA";

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
  public static final String LOGGER_TYPE = System.getProperty(LOGGER_TYPE_CONF,
      DEFAULT_LOGGER_TYPE);

  /**
   * RPC framework
   */
  public enum RPC_FRAMEWORK {
    TRADRPC, SPECRPC, GRPC
  };

  public static final String RPC_FRAMEWORK_PROPERTY = "rpc.framework";
  public static final String DEFAULT_RPC_FRAMEWORK = RPC_FRAMEWORK.TRADRPC + "";

  public static final String RPC_CONFIG_FILE_PROPERTY = "rpc.config.file";

  public static final String RPC_SPECRPC_STATISTICS_ENABLED_PROPERTY = "rpc.specrpc.statistics.enabled";
  public static final String DEFAULT_RPC_SPECRPC_STATISTICS_ENABLED = "false";

  /**
   * Benchmark configurations
   */
  // Benchmark running time in seconds. Default is 60 seconds
  // e.g. PT5h, PT5m, or PT5s means 5 hours, minutes or seconds.
  public static final String BENCHMARK_RUNNING_TIME_PROPERTY = "benchmark.running.time";
  public static final String DEFAULT_BENCHMARK_RUNNING_TIME = "PT60s";

  // Target Throughput (#Txn/s), default is 0, which means sending txns
  // back-to-back, which is the max.
  public static final String BENCHMARK_TARGET_THROUGHPUT_PROPERTY = "benchmark.throughput";
  public static final String DEFAULT_BENCHMARK_TARGET_THROUGHPUT = "0";

  /**
   * Workload configurations
   */
  // one_hop or multi_hop
  public static final String WORKLOAD_TYPE_PROPERTY = "workload.type";
  public static final String DEFAULT_WORKLOAD_TYPE = "one_hop";

  // 0 for dynamic
  public static final String RANDOM_SEED_PROPERTY = "random.seed";
  public static final String DEFAULT_RANDOM_SEED = "0";

  public static final String WORKLOAD_CLIENT_LOCAL_COMP_TIME_AFTER_RPC_PROPERTY = "workload.client.local.comp.time.afterRpc"; // ms
  public static final String DEFAULT_WORKLOAD_CLIENT_LOCAL_COMP_TIME_AFTER_RPC = "0";

  public static final String WORKLOAD_REQUEST_RPC_NUM_PROPERTY = "workload.req.rpc.num";
  public static final String DEFAULT_WORKLOAD_REQUEST_RPC_NUM = "1";

  // Number of RPC hops among servers, including the one between client and server
  public static final String WORKLOAD_REQUEST_RPC_HOP_NUM_PROPERTY = "workload.req.rpc.hop.num";
  public static final String DEFAULT_WORKLOAD_REQUEST_RPC_HOP_NUM = "1";

  public static final String WORKLOAD_REQUEST_DATA_LENGTH_PROPERTY = "workload.req.data.length"; // bytes
  public static final String DEFAULT_WORKLOAD_REQUEST_RPC_DATA_SIZE = "64";

  public static final String WORKLOAD_SERVER_COMP_TIME_BEFORE_RPC_PROPERTY = "workload.server.comp.time.beforeRpc"; // ms
  public static final String DEFAULT_WORKLOAD_SERVER_COMP_TIME_BEFORE_RPC = "0";

  public static final String WORKLOAD_SERVER_COMP_TIME_AFTER_RPC_PROPERTY = "workload.server.comp.time.afterRpc"; // ms
  public static final String DEFAULT_WORKLOAD_SERVER_COMP_TIME_AFTER_RPC = "0";

  // Total number of servers. Server ids range from 1 to num.
  public static final String WORKLOAD_SERVER_NUM_PROPERTY = "workload.server.num";
  public static final String DEFAULT_WORKLOAD_SERVER_NUM = "4";

  /**
   * Speculation configuration
   */
  // If a client predicts its RPC result
  public static final String SPEC_CLIENT_IS_PREDICT_PROPERTY = "spec.client.isPredict";
  public static final String DEFAULT_SPEC_CLIENT_IS_PREDICT = "false";

  // For each RPC
  public static final String SPEC_CLIENT_CORRECT_RATE_PROPERTY = "spec.client.correct.rate";
  public static final String DEFAULT_SPEC_CLIENT_CORRECT_RATE = "100";

  // If a server predicts its RPC result
  public static final String SPEC_SERVER_IS_PREDICT_PROPERTY = "spec.server.isPredict";
  public static final String DEFAULT_SPEC_SERVER_IS_PREDICT = "false";

  public static final String SPEC_SERVER_CORRECT_RATE_PROPERTY = "spec.server.correct.rate";
  public static final String DEFAULT_SPEC_SERVER_CORRECT_RATE = "100";

  // When a server predicts its RPC result
  public enum PREDICT_POINT {
    BEFORE_ANY, // before doing any operation
    BEFORE_RPC, // before calling the first RPC
    AFTER_RPC, // after calling the last RPC
  }

  public static final String SPEC_SERVER_PREDICT_POINT = "spec.server.predict.point";
  public static final String DEFAULT_SPEC_SERVER_PREDICT_POINT = "before_rpc";

  /**
   * Server configurations
   */
  public static final String SERVER_ID_PROPERTY = "server.id";
  public static final String SERVER_IP_PROPERTY = "server.ip";
  public static final String SERVER_PORT_PROPERTY = "server.port";
  
  /**
   * Client configurations
   */
  public static final String CLIENT_ID_PROPERTY = "client.id";

}
