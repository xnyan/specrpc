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

package rc.server.grpc;

import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.grpc.StatusRuntimeException;
import rc.common.RcConstants;
import rc.common.ServerLocationTable;
import rc.common.TxnInfo;
import rc.common.grpc.ClientStubGrpc;
import rc.grpc.AbortTxnRequest;
import rc.grpc.AbortTxnResponse;
import rc.grpc.CommitTxnRequest;
import rc.grpc.CommitTxnResponse;
import rc.grpc.PrepareTxnRequest;
import rc.grpc.PrepareTxnResponse;
import rc.grpc.VoteCommitTxnRequest;
import rc.grpc.VoteCommitTxnResponse;
import rc.grpc.RcServiceGrpc.RcServiceBlockingStub;
import rc.server.db.txn.RcTxnCoordinatorRpcFacade;

public class RcTxnCoordinatorRpcFacadeGrpc implements RcTxnCoordinatorRpcFacade {

  private static final Logger logger = LoggerFactory.getLogger(RcConstants.LOGGER_TYPE);

  private final ClientStubGrpc grpcClient;

  public RcTxnCoordinatorRpcFacadeGrpc(ServerLocationTable serverLocationTable) {
    this.grpcClient = new ClientStubGrpc(serverLocationTable);
  }

  @Override
  public Boolean prepareTxn(String serverId, TxnInfo txnInfo) {
    Boolean isPrepared = false;
    RcServiceBlockingStub rcServiceStub = this.grpcClient.getBlockingStub(serverId);
    PrepareTxnRequest request = PrepareTxnRequest.newBuilder().setTxnId(txnInfo.txnId)
        .addAllReadKey(Arrays.asList(txnInfo.getReadKeyList())).addAllWriteKey(Arrays.asList(txnInfo.getWriteKeyList()))
        .addAllWriteVal(Arrays.asList(txnInfo.getWriteValList())).build();

    try {
      PrepareTxnResponse response = rcServiceStub.prepareTxn(request);
      isPrepared = response.getIsPrepared();
    } catch (StatusRuntimeException e) {
      logger.error(e.getMessage());
      logger.error("RPC prepareTxn() failed with status = " + e.getStatus());
    }

    return isPrepared;
  }

  @Override
  public Boolean abortTxn(String serverId, String txnId) {
    Boolean isAborted = false;
    RcServiceBlockingStub rcServiceStub = this.grpcClient.getBlockingStub(serverId);
    AbortTxnRequest request = AbortTxnRequest.newBuilder().setTxnId(txnId).build();

    try {
      AbortTxnResponse response = rcServiceStub.abortTxn(request);
      isAborted = response.getIsAborted();
    } catch (StatusRuntimeException e) {
      logger.error(e.getMessage());
      logger.error("RPC abortTxn() failed with status = " + e.getStatus());
    }

    return isAborted;
  }

  @Override
  public Boolean commitTxn(String serverId, String txnId) {
    Boolean isCommitted = false;
    RcServiceBlockingStub rcServiceStub = this.grpcClient.getBlockingStub(serverId);
    CommitTxnRequest request = CommitTxnRequest.newBuilder().setTxnId(txnId).build();

    try {
      CommitTxnResponse response = rcServiceStub.commitTxn(request);
      isCommitted = response.getIsCommitted();
    } catch (StatusRuntimeException e) {
      logger.error(e.getMessage());
      logger.error("RPC commitTxn() failed with status = " + e.getStatus());
    }

    return isCommitted;
  }

  @Override
  public Boolean voteToCommitTxn(String serverId, String txnId, Boolean isVoteToCommit) {
    Boolean isConsensusMade = false;
    RcServiceBlockingStub rcServiceStub = this.grpcClient.getBlockingStub(serverId);
    VoteCommitTxnRequest request = VoteCommitTxnRequest.newBuilder().setTxnId(txnId).setIsVoteToCommit(isVoteToCommit)
        .build();

    try {
      VoteCommitTxnResponse response = rcServiceStub.voteToCommitTxn(request);
      isConsensusMade = response.getIsConsensusMade();
    } catch (StatusRuntimeException e) {
      logger.error(e.getMessage());
      logger.error("RPC voteToCommitTxn() failed with status = " + e.getStatus());
    }

    return isConsensusMade;
  }

}
