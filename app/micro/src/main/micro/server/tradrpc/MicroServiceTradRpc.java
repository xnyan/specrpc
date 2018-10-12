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

package micro.server.tradrpc;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import micro.common.MicroConstants;
import micro.server.MicroServer;
import micro.server.MicroService;
import rpc.execption.MethodNotRegisteredException;
import tradrpc.client.TradRpcUserException;
import tradrpc.client.api.TradRpcClient;
import tradrpc.client.api.TradRpcServerStub;
import tradrpc.server.TradRpcClientStub;
import tradrpc.server.api.TradRpcHost;

public class MicroServiceTradRpc extends MicroService implements TradRpcHost {

  private static final Logger logger = LoggerFactory.getLogger(MicroConstants.LOGGER_TYPE);

  private TradRpcClientStub tradRpc;

  public MicroServiceTradRpc() {

  }

  @Override
  public void bind(TradRpcClientStub clientStub) {
    this.tradRpc = clientStub;
  }

  @Override
  public String multiHop(String data, Integer hopNum) {
    this.doComputation(MicroServer.getComputationTimeBeforeRPC());

    if (hopNum > 0) {
      try {
        TradRpcServerStub rpcClientStub = TradRpcClient.bind(MicroServer.getNextHopServerId(),
            MicroService.RPC_MULTI_HOP);
        rpcClientStub.call(data, hopNum - 1); // blocking call
      } catch (MethodNotRegisteredException | IOException e) {
        e.printStackTrace();
        logger.error(e.getMessage());
        System.exit(MicroConstants.RUNTIME_FATAL_ERROR_CODE);
      } catch (TradRpcUserException e1) {
        logger.warn(e1.getMessage());
      }
    }

    this.doComputation(MicroServer.getComputationTimeAfterRPC());

    return data;
  }

}
