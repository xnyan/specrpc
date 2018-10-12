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

package micro.client.tradrpc;

import java.io.IOException;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import micro.client.MicroClient;
import micro.client.request.MicroRequest;
import micro.common.MicroConstants;
import micro.common.MicroServerIdService;
import micro.server.MicroService;
import rpc.execption.MethodNotRegisteredException;
import tradrpc.client.TradRpcUserException;
import tradrpc.client.api.TradRpcClient;
import tradrpc.client.api.TradRpcServerStub;

public class MicroClientTradRpc extends MicroClient {

  private static final Logger logger = LoggerFactory.getLogger(MicroConstants.LOGGER_TYPE);

  public MicroClientTradRpc(Properties clientConfig) {
    super(clientConfig);
  }
  
  @Override
  public void initRpcFramework(Properties config) {
    try {
      TradRpcClient.initClient(this.rpcConfigFile);
    } catch (IOException e) {
      e.printStackTrace();
      logger.error(e.getMessage());
      System.exit(MicroConstants.INIT_FAIL_ERROR_CODE);
    }
  }

  @Override
  public String execRequest(MicroRequest request) {
    String ret = null;
    int rpcNum = request.getRpcNum();

    for (int i = 1; i <= rpcNum; i++) {
      String serverId = MicroServerIdService.getServerId(i);
      String rpcRet = null;

      // Issuing an RPC
      try {
        TradRpcServerStub rpcClientStub = null;
        switch (request.getType()) {
        case ONE_HOP:
          rpcClientStub = TradRpcClient.bind(serverId, MicroService.RPC_ONE_HOP);
          rpcRet = (String) rpcClientStub.call(request.getData()); // blocking call
          break;
        case MULTI_HOP:
          int hopNumPerRpc = request.getRpcHopNum(i) - 1;
          rpcClientStub = TradRpcClient.bind(serverId, MicroService.RPC_MULTI_HOP);
          rpcRet = (String) rpcClientStub.call(request.getData(), hopNumPerRpc);
          break;
        default:
          logger.error("Unkown RPC type = ", request.getType());
          System.exit(MicroConstants.RUNTIME_FATAL_ERROR_CODE);
        }
      } catch (MethodNotRegisteredException | IOException | TradRpcUserException e) {
        e.printStackTrace();
        logger.error(e.getMessage());
        System.exit(MicroConstants.RUNTIME_FATAL_ERROR_CODE);
      }

      // Combines RPC return values
      if (ret == null) {
        ret = rpcRet;
      } else {
        ret += rpcRet;
      }

      // Does local computation
      this.doLocalComputation(request.getLocalCompTime(i));

    }

    return ret;
  }

  @Override
  public void shutdown() {
    try {
      TradRpcClient.shutdown();
    } catch (IOException e) {
      logger.error(e.getMessage());
      logger.error("Failed to shutdown TradRPC framework.");
      System.exit(MicroConstants.RUNTIME_FATAL_ERROR_CODE);
    }
  }

}
