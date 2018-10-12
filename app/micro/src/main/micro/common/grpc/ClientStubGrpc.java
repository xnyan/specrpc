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

package micro.common.grpc;

import java.util.Hashtable;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import micro.common.ServerLocationTable;
import micro.grpc.MicroServiceGrpc;
import micro.grpc.MicroServiceGrpc.MicroServiceBlockingStub;

public class ClientStubGrpc {
  
  private Hashtable<String, ManagedChannel> grpcServerChannelTable;
  private ServerLocationTable serverLocationTable;

  public ClientStubGrpc(ServerLocationTable serverLocationTable) {
    this.grpcServerChannelTable = new Hashtable<String, ManagedChannel>();
    this.serverLocationTable = serverLocationTable;
  }

  // Finds the hostname/IP address for the given server ID.
  public String lookupIp(String serverId) {
    return this.serverLocationTable.getServerHostname(serverId);
  }

  // Finds the port for the given server ID
  public int lookupPort(String serverId) {
    return this.serverLocationTable.getServerPort(serverId);
  }

  public ManagedChannel getChannelToServer(String serverId) {
    if (this.grpcServerChannelTable.containsKey(serverId)) {
      return this.grpcServerChannelTable.get(serverId);
    }

    String ip = this.lookupIp(serverId);
    int port = this.lookupPort(serverId);
    ManagedChannel channel = ManagedChannelBuilder.forAddress(ip, port).usePlaintext(true).build();
    this.grpcServerChannelTable.put(serverId, channel);

    return channel;
  }

  public MicroServiceBlockingStub getBlockingStub(String serverId) {
    ManagedChannel channel = this.getChannelToServer(serverId);
    MicroServiceBlockingStub rcServiceStub = MicroServiceGrpc.newBlockingStub(channel);
    return rcServiceStub;
  }

}
