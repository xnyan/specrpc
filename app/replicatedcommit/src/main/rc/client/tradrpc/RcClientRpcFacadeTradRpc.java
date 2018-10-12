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

package rc.client.tradrpc;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rc.client.RcClientRpcFacade;
import rc.common.RcConstants;
import rc.common.TxnReadResult;
import rc.server.RcService;
import rpc.execption.MethodNotRegisteredException;
import tradrpc.client.TradRpcUserException;
import tradrpc.client.api.TradRpcClient;
import tradrpc.client.api.TradRpcServerStub;

public class RcClientRpcFacadeTradRpc implements RcClientRpcFacade {

  private static final Logger logger = LoggerFactory.getLogger(RcConstants.LOGGER_TYPE);
  
  public RcClientRpcFacadeTradRpc() {
    
  }
  
  @Override
  public TxnReadResult read(String serverId, String txnId, String key) {
    TxnReadResult readResult = null;
    try {
      TradRpcServerStub rpcClientStub = TradRpcClient.bind(serverId, RcService.RPC_READ);
      readResult = (TxnReadResult) rpcClientStub.call(txnId, key);
    } catch (MethodNotRegisteredException | IOException | TradRpcUserException e) {
      logger.error(e.getMessage());
    }
    return readResult;
  }

  @Override
  public boolean abort(String serverId, String txnId, String[] readKeyList) {
    boolean isAborted = false;
    try {
      TradRpcServerStub rpcClientStub = TradRpcClient.bind(serverId, RcService.RPC_CLIENT_ABORT);
      isAborted = (Boolean) rpcClientStub.call(txnId, readKeyList);
    } catch (MethodNotRegisteredException | IOException | TradRpcUserException e) {
      logger.error(e.getMessage());
    }
    return isAborted;
  }

  @Override
  public boolean proposeToCommitTxn(String serverId, String txnId, String[] readKeyList, String[] writeKeyList,
      String[] writeValList) {
    boolean isAcceptToCommit = false;
    try {
      TradRpcServerStub rpcClientStub = TradRpcClient.bind(serverId, RcService.RPC_PROPOSE_TO_COMMIT_TXN);
      isAcceptToCommit = (Boolean) rpcClientStub.call(txnId, readKeyList, writeKeyList, writeValList);
    } catch (MethodNotRegisteredException | IOException | TradRpcUserException e) {
      logger.error(e.getMessage());
    }
    return isAcceptToCommit;
  }

}
