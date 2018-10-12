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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rc.client.grpc.RcClientLibGrpc;
import rc.client.specrpc.RcClientLibSpecRpc;
import rc.client.tradrpc.RcClientLibTradRpc;
import rc.common.RcConstants;
import rc.common.RcConstants.RPC_FRAMEWORK;

public class RcClient {

  private static final Logger logger = LoggerFactory.getLogger(RcConstants.LOGGER_TYPE);

  private RcClientLib rcClientLib;
  private final RPC_FRAMEWORK rpcFramework;
  
  public RcClient(Properties config) {
    String rpcFrameworkType = config.getProperty(
        RcConstants.RPC_FRAMEWORK_PROPERTY, 
        RcConstants.DEFAULT_RPC_FRAMEWORK);
    this.rpcFramework = RPC_FRAMEWORK.valueOf(rpcFrameworkType.toUpperCase());
    switch (this.rpcFramework) {
    case TRADRPC:
      this.rcClientLib = new RcClientLibTradRpc(config);
      break;
    case SPECRPC:
      this.rcClientLib = new RcClientLibSpecRpc(config);
      break;
    case GRPC:
      this.rcClientLib = new RcClientLibGrpc(config);
      break;
    default:
      logger.error("Invalid RPC framework: " + this.rpcFramework);
      System.exit(RcConstants.INIT_FAIL_ERROR_CODE);
    }
  }
  
  public RPC_FRAMEWORK getRpcFramworkType() {
    return this.rpcFramework;
  }
  
  public RcClientTxn beginTxn() {
    return this.rcClientLib.beginTxn();
  }
  
  public RcClientTxn beginTxn(String txnId) {
    return this.rcClientLib.beginTxn(txnId);
  }
  
  public void shutdown() {
    this.rcClientLib.shutdown();
  }
  
  public String getClientLibId() {
    // TODO Changes the static client lib id to non-static
    return RcClientLib.CLIENT_LIB_ID;
  }
}
