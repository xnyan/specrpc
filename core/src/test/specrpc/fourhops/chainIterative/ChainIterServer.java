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

package specrpc.fourhops.chainIterative;

import java.io.IOException;

import rpc.execption.UninitializationException;
import specrpc.common.RpcSignature;
import specrpc.server.api.SpecRpcServer;

public class ChainIterServer extends Thread {

  private final String serverIdentity;
  private final SpecRpcServer specRpcServer;

  public ChainIterServer(String serverIdentity) {
    this.serverIdentity = serverIdentity;
    this.specRpcServer = new SpecRpcServer();
    try {
      this.specRpcServer.initServer(this.serverIdentity, null); // start server-side SpecRPC framework
      registerRPCMethods(); // register RPC methods
    } catch (IOException e) {
      e.printStackTrace();
    } catch (UninitializationException e) {
      e.printStackTrace();
    }
  }

  private void registerRPCMethods() throws UninitializationException {
    ChainIterServiceHostFactory chainIterServiceHostFactory = new ChainIterServiceHostFactory();

    specRpcServer.register(ChainIterServiceHost.FIRST_MULTI_SPEC_RETURN, chainIterServiceHostFactory, String.class,
        String.class);
    specRpcServer.register(ChainIterServiceHost.SECOND_MULTI_SPEC_RETURN, chainIterServiceHostFactory, String.class,
        String.class);
    specRpcServer.register(ChainIterServiceHost.THIRD_MULTI_SPEC_RETURN, chainIterServiceHostFactory, String.class,
        String.class);
    specRpcServer.register(ChainIterServiceHost.END_MULTI_SPEC_RETURN, chainIterServiceHostFactory, String.class,
        String.class);

    specRpcServer.register(ChainIterServiceHost.FIRST_CLIENT_SPEC, chainIterServiceHostFactory, String.class,
        String.class);
    specRpcServer.register(ChainIterServiceHost.SECOND_CLIENT_SPEC, chainIterServiceHostFactory, String.class,
        String.class);
    specRpcServer.register(ChainIterServiceHost.THIRD_CLIENT_SPEC, chainIterServiceHostFactory, String.class,
        String.class);
    specRpcServer.register(ChainIterServiceHost.END_RETURN, chainIterServiceHostFactory, String.class, String.class);

    specRpcServer.register(ChainIterServiceHost.FIRST_MID_SERVER_SPEC, chainIterServiceHostFactory, String.class,
        String.class);
    specRpcServer.register(ChainIterServiceHost.SECOND_MID_SERVER_SPEC, chainIterServiceHostFactory, String.class,
        String.class);
    specRpcServer.register(ChainIterServiceHost.THIRD_MID_SERVER_SPEC, chainIterServiceHostFactory, String.class,
        String.class);
  }

  public void run() {
    try {
      specRpcServer.execute();
    } catch (UninitializationException | InterruptedException | IOException e) {
      e.printStackTrace();
    } 
  }

  public void terminate() {
    try {
      specRpcServer.terminate();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public static RpcSignature getFirstMultiSpecReturn() {
    return new RpcSignature(ChainIterServiceHost.class.getName(), ChainIterServiceHost.FIRST_MULTI_SPEC_RETURN,
        String.class, String.class);
  }

  public static RpcSignature getSecondMultiSpecReturn() {
    return new RpcSignature(ChainIterServiceHost.class.getName(), ChainIterServiceHost.SECOND_MULTI_SPEC_RETURN,
        String.class, String.class);
  }

  public static RpcSignature getThirdMultiSpecReturn() {
    return new RpcSignature(ChainIterServiceHost.class.getName(), ChainIterServiceHost.THIRD_MULTI_SPEC_RETURN,
        String.class, String.class);
  }

  public static RpcSignature getEndMultiSpecReturn() {
    return new RpcSignature(ChainIterServiceHost.class.getName(), ChainIterServiceHost.END_MULTI_SPEC_RETURN,
        String.class, String.class);
  }

  public static RpcSignature getFirstClientSpec() {
    return new RpcSignature(ChainIterServiceHost.class.getName(), ChainIterServiceHost.FIRST_CLIENT_SPEC,
        String.class, String.class);
  }

  public static RpcSignature getSecondClientSpec() {
    return new RpcSignature(ChainIterServiceHost.class.getName(), ChainIterServiceHost.SECOND_CLIENT_SPEC,
        String.class, String.class);
  }

  public static RpcSignature getThirdClientSpec() {
    return new RpcSignature(ChainIterServiceHost.class.getName(), ChainIterServiceHost.THIRD_CLIENT_SPEC,
        String.class, String.class);
  }

  public static RpcSignature getEndReturn() {
    return new RpcSignature(ChainIterServiceHost.class.getName(), ChainIterServiceHost.END_RETURN, String.class,
        String.class);
  }

  public static RpcSignature getFirstMidServerSpec() {
    return new RpcSignature(ChainIterServiceHost.class.getName(), ChainIterServiceHost.FIRST_MID_SERVER_SPEC,
        String.class, String.class);
  }

  public static RpcSignature getSecondMidServerSpec() {
    return new RpcSignature(ChainIterServiceHost.class.getName(), ChainIterServiceHost.SECOND_MID_SERVER_SPEC,
        String.class, String.class);
  }

  public static RpcSignature getThirdMidServerSpec() {
    return new RpcSignature(ChainIterServiceHost.class.getName(), ChainIterServiceHost.THIRD_MID_SERVER_SPEC,
        String.class, String.class);
  }
}
