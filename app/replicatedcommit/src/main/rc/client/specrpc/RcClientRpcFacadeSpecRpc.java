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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rc.client.RcClientRpcFacade;
import rc.common.RcConstants;
import rc.common.TxnReadResult;
import rc.common.specrpc.EmptyCallbackFactory;
import rc.server.RcService;
import rpc.execption.MethodNotRegisteredException;
import rpc.execption.UserException;
import specrpc.client.api.SpecRpcClient;
import specrpc.client.api.SpecRpcClientStub;
import specrpc.exception.SpeculationFailException;

public class RcClientRpcFacadeSpecRpc implements RcClientRpcFacade {

  private static final Logger logger = LoggerFactory.getLogger(RcConstants.LOGGER_TYPE);
  
  private static final EmptyCallbackFactory emptyCallbackFactory = new EmptyCallbackFactory();

  @Override
  public TxnReadResult read(String serverId, String txnId, String key) {
    TxnReadResult txnReadResult = null;
    try {
      SpecRpcClientStub rpcStub = SpecRpcClient.bind(serverId, RcService.RPC_READ);
      txnReadResult = (TxnReadResult) rpcStub.call(null, emptyCallbackFactory, txnId, key).getResult();
    } catch (MethodNotRegisteredException | IOException | InterruptedException | UserException e) {
      logger.error(e.getMessage());
    } catch (SpeculationFailException e) {
      ; // Does nothing
    }
    return txnReadResult;
  }

  @Override
  public boolean abort(String serverId, String txnId, String[] readKeyList) {
    boolean isAbort = false;
    SpecRpcClientStub rpcStub;
    try {
      rpcStub = SpecRpcClient.bind(serverId, RcService.RPC_CLIENT_ABORT);
      isAbort = (Boolean) rpcStub.call(null, emptyCallbackFactory, txnId, readKeyList).getResult();
    } catch (MethodNotRegisteredException | IOException | InterruptedException | UserException e) {
      logger.error(e.getMessage());
    } catch (SpeculationFailException e) {
      ; // Does nothing
    }

    return isAbort;
  }

  @Override
  public boolean proposeToCommitTxn(String serverId, String txnId, String[] readKeyList, String[] writeKeyList,
      String[] writeValList) {
    boolean isAcceptToCommit = false;
    SpecRpcClientStub rpcStub;
    try {
      rpcStub = SpecRpcClient.bind(serverId, RcService.RPC_PROPOSE_TO_COMMIT_TXN);
      isAcceptToCommit = (Boolean) rpcStub
          .call(null, emptyCallbackFactory, txnId, readKeyList, writeKeyList, writeValList)
          .getResult();
    } catch (MethodNotRegisteredException | IOException | InterruptedException | UserException e) {
      logger.error(e.getMessage());
    } catch (SpeculationFailException e) {
      ; // Does nothing
    }
    return isAcceptToCommit;
  }

}
