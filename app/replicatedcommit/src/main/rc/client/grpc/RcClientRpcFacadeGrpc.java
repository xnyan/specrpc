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

import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.grpc.StatusRuntimeException;
import rc.client.RcClientRpcFacade;
import rc.common.RcConstants;
import rc.common.ServerLocationTable;
import rc.common.TxnReadResult;
import rc.common.grpc.ClientStubGrpc;
import rc.grpc.TxnReadRequest;
import rc.grpc.TxnReadResponse;
import rc.grpc.ClientAbortTxnRequest;
import rc.grpc.ClientAbortTxnResponse;
import rc.grpc.ProposeCommitTxnRequest;
import rc.grpc.ProposeCommitTxnResponse;
import rc.grpc.RcServiceGrpc.RcServiceBlockingStub;

public class RcClientRpcFacadeGrpc implements RcClientRpcFacade {

  private static final Logger logger = LoggerFactory.getLogger(RcConstants.LOGGER_TYPE);

  private final ClientStubGrpc grpcClient;

  public RcClientRpcFacadeGrpc(ServerLocationTable serverLocationTable) {
    this.grpcClient = new ClientStubGrpc(serverLocationTable);
  }

  @Override
  public TxnReadResult read(String serverId, String txnId, String key) {
    TxnReadResult readResult = null;
    RcServiceBlockingStub rcServiceStub = this.grpcClient.getBlockingStub(serverId);
    TxnReadRequest request = TxnReadRequest.newBuilder().setTxnId(txnId).setReadKey(key).build();

    try {
      TxnReadResponse response = rcServiceStub.read(request);
      readResult = new TxnReadResult(response.getValue(), response.getVersion(), response.getIsSharedLockAcquired());
    } catch (StatusRuntimeException e) {
      logger.error(e.getMessage());
      logger.error("RPC read() failed with status = " + e.getStatus());
    }

    return readResult;
  }

  @Override
  public boolean abort(String serverId, String txnId, String[] readKeyList) {
    boolean isAborted = false;
    RcServiceBlockingStub rcServiceStub = this.grpcClient.getBlockingStub(serverId);
    ClientAbortTxnRequest request = ClientAbortTxnRequest.newBuilder().setTxnId(txnId)
        .addAllReadKey(Arrays.asList(readKeyList)).build();

    try {
      ClientAbortTxnResponse response = rcServiceStub.clientAbortTxn(request);
      isAborted = response.getIsAborted();
    } catch (StatusRuntimeException e) {
      logger.error(e.getMessage());
      logger.error("RPC clientAbortTxn() failed with status = " + e.getStatus());
    }

    return isAborted;
  }

  @Override
  public boolean proposeToCommitTxn(String serverId, String txnId, String[] readKeyList, String[] writeKeyList,
      String[] writeValList) {
    boolean isAcceptToCommit = false;
    RcServiceBlockingStub rcServiceStub = this.grpcClient.getBlockingStub(serverId);
    ProposeCommitTxnRequest request = ProposeCommitTxnRequest.newBuilder().setTxnId(txnId)
        .addAllReadKey(Arrays.asList(readKeyList)).addAllWriteKey(Arrays.asList(writeKeyList))
        .addAllWriteVal(Arrays.asList(writeValList)).build();

    try {
      ProposeCommitTxnResponse response = rcServiceStub.proposeToCommitTxn(request);
      isAcceptToCommit = response.getIsToCommit();
    } catch (StatusRuntimeException e) {
      logger.error(e.getMessage());
      logger.error("RPC proposeToCommitTxn() failed with status = " + e.getStatus());
    }

    return isAcceptToCommit;
  }

}
