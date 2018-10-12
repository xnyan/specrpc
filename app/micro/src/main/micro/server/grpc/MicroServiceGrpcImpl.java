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

package micro.server.grpc;

import micro.grpc.MicroServiceGrpc;
import micro.grpc.Response;

public class MicroServiceGrpcImpl extends MicroServiceGrpc.MicroServiceImplBase {

  /**
   * oneHop RPC
   */
  public void oneHop(micro.grpc.OneHopRequest request,
      io.grpc.stub.StreamObserver<micro.grpc.Response> responseObserver) {
    MicroServiceGrpcInstance microService = new MicroServiceGrpcInstance();
    String ret = microService.oneHop(request.getData());

    Response response = Response.newBuilder().setData(ret).build();

    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }

  /**
   * multiHop RPC
   */
  public void multiHop(micro.grpc.MultiHopRequest request,
      io.grpc.stub.StreamObserver<micro.grpc.Response> responseObserver) {
    MicroServiceGrpcInstance microService = new MicroServiceGrpcInstance();
    String ret = microService.multiHop(request.getData(), request.getHopNum());

    Response response = Response.newBuilder().setData(ret).build();

    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }
}
