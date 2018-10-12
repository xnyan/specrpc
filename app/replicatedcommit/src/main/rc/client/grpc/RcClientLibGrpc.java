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

package rc.client.grpc;

import java.io.IOException;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rc.client.RcClientLib;
import rc.client.RcClientTxn;
import rc.common.RcConstants;
import rc.common.ServerLocationTable;
import rpc.config.Constants;
import specrpc.common.RpcConfig;

public class RcClientLibGrpc extends RcClientLib {

  private static final Logger logger = LoggerFactory.getLogger(RcConstants.LOGGER_TYPE);

  public RcClientLibGrpc(Properties clientConfig) {
    super(clientConfig);
  }

  @Override
  public RcClientTxn beginTxn(String txnId) {
    return new RcClientTxnGrpc(txnId);
  }

  @Override
  protected void initRpcFramework(Properties config) {
    try {
      // Reuses SpecRPC's RPC signature file to locate RPC servers' ip addresses and ports
      RpcConfig rpcConfig = new RpcConfig(rpcConfigFile);
      String rpcSigFile = rpcConfig.get(Constants.RPC_HOST_SIGNATURE_FILE_PROPERTY,
          Constants.DEFAULT_RPC_HOST_SIGNATURE_FILE);
       
      RcClientLib.SERVER_LOCATION_TABLE = new ServerLocationTable(rpcSigFile);
    } catch (IOException e) {
      logger.error(e.getMessage());
      e.printStackTrace();
      System.exit(RcConstants.INIT_FAIL_ERROR_CODE);
    }
  }

  @Override
  public void shutdown() {
    // Does nothing
  }

}
