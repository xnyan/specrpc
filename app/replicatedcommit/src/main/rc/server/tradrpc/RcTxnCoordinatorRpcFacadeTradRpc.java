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

package rc.server.tradrpc;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rc.common.RcConstants;
import rc.common.TxnInfo;
import rc.server.RcService;
import rc.server.db.txn.RcTxnCoordinatorRpcFacade;
import rpc.execption.MethodNotRegisteredException;
import tradrpc.client.TradRpcUserException;
import tradrpc.client.api.TradRpcClient;
import tradrpc.client.api.TradRpcServerStub;

public class RcTxnCoordinatorRpcFacadeTradRpc implements RcTxnCoordinatorRpcFacade {

  private static final Logger logger = LoggerFactory.getLogger(RcConstants.LOGGER_TYPE);
  
  public RcTxnCoordinatorRpcFacadeTradRpc() {
  }

  @Override
  public Boolean prepareTxn(String serverId, TxnInfo txnInfo) {
    Boolean isPrepared = false;
    try {
      TradRpcServerStub rpcClientStub = TradRpcClient.bind(serverId, RcService.RPC_PREPARE_TXN);
      isPrepared = (Boolean) rpcClientStub.call(
          txnInfo.txnId,
          txnInfo.getReadKeyList(), 
          txnInfo.getWriteKeyList(), 
          txnInfo.getWriteValList());
    } catch (MethodNotRegisteredException | IOException | TradRpcUserException e) {
      logger.error(e.getMessage());
    }
    return isPrepared;
  }
  
  @Override
  public Boolean abortTxn(String serverId, String txnId) {
    Boolean res = false;
    TradRpcServerStub rpcClientStub;
    try {
      rpcClientStub = TradRpcClient.bind(serverId, RcService.RPC_ABORT_TXN);
      res = (Boolean) rpcClientStub.call(txnId);
    } catch (MethodNotRegisteredException | IOException | TradRpcUserException e) {
      logger.error(e.getMessage());
    }
    
    return res;
  }

  @Override
  public Boolean commitTxn(String serverId, String txnId) {
    Boolean res = false;
    TradRpcServerStub rpcClientStub;
    try {
      rpcClientStub = TradRpcClient.bind(serverId, RcService.RPC_COMMIT_TXN);
      res = (Boolean) rpcClientStub.call(txnId);
    } catch (MethodNotRegisteredException | IOException | TradRpcUserException e) {
      logger.error(e.getMessage());
    }
    
    return res;
  }

  @Override
  public Boolean voteToCommitTxn(String serverId, String txnId, Boolean isVoteToCommit) {
    Boolean res = false;
    TradRpcServerStub rpcClientStub;
    try {
      rpcClientStub = TradRpcClient.bind(serverId, RcService.RPC_VOTE_TO_COMMIT_TXN);
      res = (Boolean) rpcClientStub.call(txnId, isVoteToCommit);
    } catch (MethodNotRegisteredException | IOException | TradRpcUserException e) {
      logger.error(e.getMessage());
    }
    
    return res;
  }
}
