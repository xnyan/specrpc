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

import rc.common.RcConstants;
import rc.common.TxnReadResult;
import rc.grpc.AbortTxnResponse;
import rc.grpc.ClientAbortTxnResponse;
import rc.grpc.CommitTxnResponse;
import rc.grpc.IsDoneResponse;
import rc.grpc.PrepareTxnResponse;
import rc.grpc.ProposeCommitTxnResponse;
import rc.grpc.RcServiceGrpc;
import rc.grpc.TxnReadResponse;
import rc.grpc.VoteCommitTxnResponse;

public class RcServiceGrpcImpl extends RcServiceGrpc.RcServiceImplBase {
  /**
   * <pre>
   * RPCs provided by non-coordinator servers for clients to call
   * </pre>
   */
  public void read(rc.grpc.TxnReadRequest request,
      io.grpc.stub.StreamObserver<rc.grpc.TxnReadResponse> responseObserver) {
    RcServiceGrpcInstance rcService = new RcServiceGrpcInstance();
    TxnReadResult res = rcService.read(request.getTxnId(), request.getReadKey());

    TxnReadResponse response = TxnReadResponse.newBuilder()
        .setValue(res.val == null ? RcConstants.READ_NULL_VALUE : res.val)
        .setVersion(res.version == null ? RcConstants.INVALID_DATA_VERSION : res.version)
        .setIsSharedLockAcquired(res.isSharedLockAcquired).build();

    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }

  /**
   */
  public void clientAbortTxn(rc.grpc.ClientAbortTxnRequest request,
      io.grpc.stub.StreamObserver<rc.grpc.ClientAbortTxnResponse> responseObserver) {
    RcServiceGrpcInstance rcService = new RcServiceGrpcInstance();
    Boolean isAborted = rcService.clientAbortTxn(request.getTxnId(),
        request.getReadKeyList().toArray(RcConstants.STRING_ARRAY_TYPE_HELPER));

    ClientAbortTxnResponse response = ClientAbortTxnResponse.newBuilder().setIsAborted(isAborted).build();

    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }

  /**
   * <pre>
   * RPCs provided by non-coordinator servers for coordinators to call
   * </pre>
   */
  public void prepareTxn(rc.grpc.PrepareTxnRequest request,
      io.grpc.stub.StreamObserver<rc.grpc.PrepareTxnResponse> responseObserver) {
    RcServiceGrpcInstance rcService = new RcServiceGrpcInstance();
    Boolean isPrepared = rcService.prepareTxn(request.getTxnId(),
        request.getReadKeyList().toArray(RcConstants.STRING_ARRAY_TYPE_HELPER),
        request.getWriteKeyList().toArray(RcConstants.STRING_ARRAY_TYPE_HELPER),
        request.getWriteValList().toArray(RcConstants.STRING_ARRAY_TYPE_HELPER));

    PrepareTxnResponse response = PrepareTxnResponse.newBuilder().setIsPrepared(isPrepared).build();

    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }

  /**
   */
  public void abortTxn(rc.grpc.AbortTxnRequest request,
      io.grpc.stub.StreamObserver<rc.grpc.AbortTxnResponse> responseObserver) {
    RcServiceGrpcInstance rcService = new RcServiceGrpcInstance();
    Boolean isAborted = rcService.abortTxn(request.getTxnId());

    AbortTxnResponse response = AbortTxnResponse.newBuilder().setIsAborted(isAborted).build();

    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }

  /**
   */
  public void commitTxn(rc.grpc.CommitTxnRequest request,
      io.grpc.stub.StreamObserver<rc.grpc.CommitTxnResponse> responseObserver) {
    RcServiceGrpcInstance rcService = new RcServiceGrpcInstance();
    Boolean isCommitted = rcService.commitTxn(request.getTxnId());

    CommitTxnResponse response = CommitTxnResponse.newBuilder().setIsCommitted(isCommitted).build();

    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }

  /**
   * <pre>
   * RPCs provided by coordinators for clients to call
   * </pre>
   */
  public void proposeToCommitTxn(rc.grpc.ProposeCommitTxnRequest request,
      io.grpc.stub.StreamObserver<rc.grpc.ProposeCommitTxnResponse> responseObserver) {
    RcServiceGrpcInstance rcService = new RcServiceGrpcInstance();
    Boolean isToCommit = rcService.proposeToCommitTxn(request.getTxnId(),
        request.getReadKeyList().toArray(RcConstants.STRING_ARRAY_TYPE_HELPER),
        request.getWriteKeyList().toArray(RcConstants.STRING_ARRAY_TYPE_HELPER),
        request.getWriteValList().toArray(RcConstants.STRING_ARRAY_TYPE_HELPER));

    ProposeCommitTxnResponse response = ProposeCommitTxnResponse.newBuilder().setIsToCommit(isToCommit).build();

    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }

  /**
   * <pre>
   * RPCs provided by coordinators for other coordinators to call
   * </pre>
   */
  public void voteToCommitTxn(rc.grpc.VoteCommitTxnRequest request,
      io.grpc.stub.StreamObserver<rc.grpc.VoteCommitTxnResponse> responseObserver) {
    RcServiceGrpcInstance rcService = new RcServiceGrpcInstance();
    Boolean isConsensusMade = rcService.voteToCommitTxn(request.getTxnId(), request.getIsVoteToCommit());
    
    VoteCommitTxnResponse response = VoteCommitTxnResponse.newBuilder().setIsConsensusMade(isConsensusMade).build();
    
    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }

  /**
   * <pre>
   * Database helper RPCs
   * </pre>
   */
  public void parseAndLoadData(rc.grpc.DataFileInfo request,
      io.grpc.stub.StreamObserver<rc.grpc.IsDoneResponse> responseObserver) {
    RcServiceGrpcInstance rcService = new RcServiceGrpcInstance();
    Boolean res = rcService.parseAndLoadData(request.getDataFile());
    
    IsDoneResponse response = IsDoneResponse.newBuilder().setIsDone(res).build();

    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }

  /**
   */
  public void dumpOrSyncData(rc.grpc.DataFileInfo request,
      io.grpc.stub.StreamObserver<rc.grpc.IsDoneResponse> responseObserver) {
    RcServiceGrpcInstance rcService = new RcServiceGrpcInstance();
    Boolean res = rcService.dumpOrSyncData(request.getDataFileDir(), request.getDataFile());
    
    IsDoneResponse response = IsDoneResponse.newBuilder().setIsDone(res).build();
    
    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }

}
