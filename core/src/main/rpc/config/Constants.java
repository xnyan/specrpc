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

package rpc.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Constants {

  public static final String LOGGER_TYPE_CONF = "logger.type";
  public static final String DEFAULT_LOGGER_TYPE = "Console";
  public static final String LOGGER_TYPE = System.getProperty(LOGGER_TYPE_CONF, DEFAULT_LOGGER_TYPE);

  private static final Logger logger = LoggerFactory.getLogger(Constants.LOGGER_TYPE);

  public static final String RPC_TYPE_CONF = "rpc.type";
  public static final String DEFAULT_RPC_TYPE = "specrpc";

  public static enum RPC_TYPE {
    SPECRPC, TRADRPC
  };

  public static final String RPC_HOME_CONF = "SPECRPC_HOME";
  public static final String RPC_HOME = System.getenv().get(RPC_HOME_CONF);
  public static final String DEFAULT_RPC_CONFIG_FILE = (RPC_HOME == null ? ("$" + RPC_HOME_CONF) : RPC_HOME)
      + "/conf/rpc-defaults.conf";

  public static String getSpecRpcHome() {
    if (RPC_HOME == null) {
      logger.error("$" + RPC_HOME_CONF + " is not set");
      return "$" + RPC_HOME_CONF;
    }
    return RPC_HOME;
  }

  // RPC configurations
  public static final String RPC_HOST_ID_PROPERTY = "rpc.host.id";
  public static final String RPC_HOST_IP_PROPERTY = "rpc.host.ip";
  public static final String RPC_HOST_PORT_PROPERTY = "rpc.host.port";
  public static final String RPC_HOST_THREADPOOL_SIZE_PROPERTY = "rpc.host.threadpool.size";
  // Max number of connections but OS implementation specific
  public static final String RPC_HOST_MAX_CONNECTION_PROPERTY = "rpc.host.connection.max";
  public static final String RPC_HOST_SIGNATURE_FILE_PROPERTY = "rpc.host.signatures"; // file location for rpc
                                                                                       // signatures

  // SpecRPC client configurations
  public static final String SPECRPC_CLIENT_THREADPOOL_SIZE_PROPERTY = "specrpc.client.threadpool.size";

  // SpecRPC statistics configurations
  public static final String SPECRPC_STATISTICS_ENABLED_PROPERTY = "specrpc.statistics.enabled";
  public static final String SPECRPC_STATISTICS_INCORRECT_PREDICTION_ENABLED_PROPERTY = "specrpc.statistics.incorrectPrediction.enabled";

  // Default configurations
  public static final String DEFAULT_RPC_HOST_ID = null;
  public static final String DEFAULT_RPC_HOST_IP = "localhost";
  public static final String DEFAULT_RPC_HOST_PORT = "0"; // dynamic port
  public static final String DEFAULT_RPC_HOST_THREADPOOL_SIZE = "0"; // dynamic-size threadpool
  public static final String DEFAULT_RPC_HOST_MAX_CONNECTION = "1024";// OS implementation specific
  public static final String DEFAULT_RPC_HOST_SIGNATURE_FILE = (RPC_HOME == null ? ("$" + RPC_HOME_CONF) : RPC_HOME)
      + "/conf/rpc.signature";
  public static final String DEFAULT_SPECRPC_CLIENT_THREADPOOL_SIZE = "0";
  public static final String DEFAULT_SPECRPC_STATISTICS_ENABLE = "false";
  public static final String DEFAULT_SPECRPC_STATISTICS_INCORRECT_PREDICTION_COUNTING = "false";

}
