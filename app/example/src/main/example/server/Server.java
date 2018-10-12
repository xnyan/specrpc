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

package example.server;

import java.io.IOException;

import example.server.specrpc.ServiceSpecRpcFactory;
import rpc.execption.UninitializationException;
import rpc.server.api.RpcHostObjectFactory;
import rpc.server.api.RpcServer;
import specrpc.server.api.SpecRpcServer;

public class Server implements Runnable {

  private final String serverId;
  private RpcServer rpcServer;

  public Server(String id, String ip, int port, String rpcConfigFile) {
    this.serverId = id;
    RpcHostObjectFactory rpcHostObjectFactory = null;
    rpcHostObjectFactory = new ServiceSpecRpcFactory();
    this.rpcServer = new SpecRpcServer();

    try {
      // Initializes RPC framework
      this.rpcServer.initServer(this.serverId, ip, port, rpcConfigFile);
      // Registers (i.e. publishes) RPC methods for clients to call
      this.rpcServer.register(Service.COMMIT.methodName, rpcHostObjectFactory, Service.COMMIT.returnType,
          Service.COMMIT.argTypes);

    } catch (IOException | UninitializationException e) {
      e.printStackTrace();
    }

  }

  @Override
  public void run() {
    // Starts up RPC framework and waits for clients to call RPCs
    try {
      this.rpcServer.execute();
    } catch (UninitializationException | InterruptedException | IOException e) {
      e.printStackTrace();
    }
  }

  public static void main(String args[]) {
    Server server = new Server("1", "localhost", 3000, null);
    Thread serverThread = new Thread(server);
    serverThread.start();
  }
}
