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

package rc.client.specrpc;

import java.io.IOException;
import java.util.HashMap;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rc.client.RcClientLib;
import rc.client.RcClientTxn;
import rc.common.RcConstants;
import specrpc.client.api.SpecRpcClient;
import specrpc.common.Location;

public class RcClientLibSpecRpc extends RcClientLib {

  private static final Logger logger = LoggerFactory.getLogger(RcConstants.LOGGER_TYPE);

  private RcClientReadProxy readProxy;
  
  public RcClientLibSpecRpc(Properties clientConfig) {
    super(clientConfig);
  }

  @Override
  public RcClientTxn beginTxn(String txnId) {
    return new RcClientTxnSpecRpc(txnId);
  }

  @Override
  protected void initRpcFramework(Properties config) {
    try {
      // Starts local read proxy server
      int port = Integer.parseInt(config.getProperty(
          RcConstants.RPC_SPECRPC_READ_PROXY_PORT_PROPERTY,
          RcConstants.DEFAULT_RPC_SPECRPC_READ_PROXY_PORT));
      this.readProxy = new RcClientReadProxy(CLIENT_LIB_ID, port, config);
      Thread readProxyThread = new Thread(readProxy);
      readProxyThread.start();
      // Waits for the read proxy to start
      this.readProxy.waitRpcServerToStart();
      long readProxyStartUpTime = Long.parseLong(config.getProperty(
          RcConstants.CLIENT_LIB_SPECRPC_READ_PROXY_START_TIME_PROPERTY,
          RcConstants.DEFAULT_CLIENT_LIB_SPECRPC_READ_PROXY_START_TIME));
      Thread.sleep(readProxyStartUpTime); // Waits for the SpecRPC framework to start up
      logger.debug("Client id = " + CLIENT_LIB_ID + " started a read proxy with SpecRPC.");
      // Initializes client-side lib
      SpecRpcClient.initClient(rpcConfigFile);
      // Loads the RPC of the readProxy
      SpecRpcClient.setRpcSig(this.readProxy.getRpcSigLocationTable());
    } catch (IOException | InterruptedException e) {
      logger.error(e.getMessage());
      System.exit(RcConstants.INIT_FAIL_ERROR_CODE);
    }
  }
  
  public void debugRpcSigLocation(HashMap<String, Location> table) {
    for (String id : table.keySet()) {
      logger.debug(id + "==>" + table.get(id).toString());
    }
  }

  @Override
  public void shutdown() {
    SpecRpcClient.shutdown();
    try {
      this.readProxy.shutdown();
    } catch (IOException e) {
      logger.error(e.getMessage());
      logger.error("Failed to shutdown RPC framework: " + this.rpcFramework);
      System.exit(RcConstants.RUNTIME_FATAL_ERROR_CODE);
    }
  }
}
