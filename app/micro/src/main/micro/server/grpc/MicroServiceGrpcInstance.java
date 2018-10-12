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

import micro.grpc.MicroServiceGrpc.MicroServiceBlockingStub;
import micro.grpc.MultiHopRequest;
import micro.server.MicroServer;
import micro.server.MicroService;

public class MicroServiceGrpcInstance extends MicroService {

  @Override
  public String multiHop(String data, Integer hopNum) {
    this.doComputation(MicroServer.getComputationTimeBeforeRPC());

    if (hopNum > 0) {
      String serverId = MicroServer.getNextHopServerId();
      MicroServiceBlockingStub microServiceStub = MicroGrpcServer.grpcClientStub.getBlockingStub(serverId);
      // Calls the RPC
      MultiHopRequest request = MultiHopRequest.newBuilder().setData(data).setHopNum(hopNum - 1).build();
      microServiceStub.multiHop(request); // blocking call
    }

    this.doComputation(MicroServer.getComputationTimeAfterRPC());

    return data;
  }

}
