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

package tradrpc.client.api;

import java.io.FileNotFoundException;
import java.io.IOException;

import rpc.communication.Communication;
import rpc.config.Constants;
import rpc.execption.MethodNotRegisteredException;
import specrpc.common.RpcConfig;
import specrpc.common.Location;
import specrpc.common.RpcSignature;
import specrpc.common.ServerLocationDirectory;
import tradrpc.client.TradRpcServerStubObject;

public class TradRpcClient {

  private static ServerLocationDirectory serverLocationDir;
  private static boolean terminated = true;

  public synchronized static void initClient(String configFile) throws FileNotFoundException, IOException {
    if (terminated == false) {
      // Has been initialized
      return;
    }

    Communication.initClientCommunication();
    RpcConfig config = new RpcConfig(configFile);
    serverLocationDir = new ServerLocationDirectory(
        config.get(Constants.RPC_HOST_SIGNATURE_FILE_PROPERTY, Constants.DEFAULT_RPC_HOST_SIGNATURE_FILE));
    terminated = false;
  }

  public synchronized static TradRpcServerStub bind(String serverIdentity, RpcSignature signature)
      throws MethodNotRegisteredException, FileNotFoundException, IOException {
    if (terminated == true) {
      return null;
    }

    Location serverLocation = lookup(serverIdentity, signature);
    TradRpcServerStub serverStub = new TradRpcServerStubObject(signature, serverLocation);
    return serverStub;
  }

  public synchronized static Location lookup(String serverIdentity, RpcSignature signature)
      throws MethodNotRegisteredException, FileNotFoundException, IOException {
    if (terminated == true) {
      return null;
    }

    if (serverLocationDir == null) {
      System.err.println("ComRPC Client does not initialize.");
      throw new MethodNotRegisteredException(null);
    }

    Location serverLocation = serverLocationDir.lookUp(serverIdentity, signature);
    return serverLocation;
  }

  public static synchronized void shutdown() throws IOException {
    if (terminated == true) {
      return;
    }
    terminated = true;
    Communication.shutdown();
  }
}
