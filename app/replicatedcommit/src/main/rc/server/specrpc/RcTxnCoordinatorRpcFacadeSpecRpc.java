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

package rc.server.specrpc;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rc.common.RcConstants;
import rc.common.TxnInfo;
import rc.common.specrpc.EmptyCallbackFactory;
import rc.server.RcService;
import rc.server.db.txn.RcTxnCoordinatorRpcFacade;
import rpc.execption.MethodNotRegisteredException;
import rpc.execption.UserException;
import specrpc.client.api.SpecRpcClient;
import specrpc.client.api.SpecRpcClientStub;
import specrpc.exception.SpeculationFailException;

public class RcTxnCoordinatorRpcFacadeSpecRpc implements RcTxnCoordinatorRpcFacade {

  private static final Logger logger = LoggerFactory.getLogger(RcConstants.LOGGER_TYPE);
  
  private static final EmptyCallbackFactory emptyCallbackFactory = new EmptyCallbackFactory();
  
  public RcTxnCoordinatorRpcFacadeSpecRpc() {
    
  }
  
  @Override
  public Boolean prepareTxn(String serverId, TxnInfo txnInfo) {
    Boolean isPrepared = false;
    try {
      SpecRpcClientStub rpcClientStub = SpecRpcClient.bind(serverId, RcService.RPC_PREPARE_TXN);
      isPrepared = (Boolean) rpcClientStub.call(
          null,
          emptyCallbackFactory,
          txnInfo.txnId,
          txnInfo.getReadKeyList(), 
          txnInfo.getWriteKeyList(), 
          txnInfo.getWriteValList()).getResult();
    } catch (MethodNotRegisteredException | IOException | InterruptedException | UserException e) {
      logger.error(e.getMessage());
    } catch (SpeculationFailException e) {
      ; // Does nothing
    }
    
    return isPrepared;
  }

  @Override
  public Boolean abortTxn(String serverId, String txnId) {
    Boolean res = false;
    try {
      SpecRpcClientStub rpcClientStub = SpecRpcClient.bind(serverId, RcService.RPC_ABORT_TXN);
      res = (Boolean) rpcClientStub.call(null, emptyCallbackFactory, txnId).getResult();
    } catch (MethodNotRegisteredException | IOException | InterruptedException | UserException e) {
      logger.error(e.getMessage());
    } catch (SpeculationFailException e) {
      ; // Does nothing
    }
    
    return res;
  }

  @Override
  public Boolean commitTxn(String serverId, String txnId) {
    Boolean res = false;
    try {
      SpecRpcClientStub rpcClientStub = SpecRpcClient.bind(serverId, RcService.RPC_COMMIT_TXN);
      res = (Boolean) rpcClientStub.call(null, emptyCallbackFactory, txnId).getResult();
    } catch (MethodNotRegisteredException | IOException | InterruptedException | UserException e) {
      logger.error(e.getMessage());
    } catch (SpeculationFailException e) {
      ; // Does nothing
    }
    
    return res;
  }

  @Override
  public Boolean voteToCommitTxn(String serverId, String txnId, Boolean isVoteToCommit) {
    Boolean res = false;
    try {
      SpecRpcClientStub rpcClientStub = SpecRpcClient.bind(serverId, RcService.RPC_VOTE_TO_COMMIT_TXN);
      res = (Boolean) rpcClientStub.call(null, emptyCallbackFactory, txnId, isVoteToCommit).getResult();
    } catch (MethodNotRegisteredException | IOException | InterruptedException | UserException e) {
      logger.error(e.getMessage());
    } catch (SpeculationFailException e) {
      ; // Does nothing
    }
    
    return res;
  }

}
