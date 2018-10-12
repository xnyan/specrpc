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

package specrpc.onehop;

import java.io.IOException;

import rpc.execption.UninitializationException;
import specrpc.server.api.SpecRpcServer;

public class OneHopServer extends Thread {

  private final String serverIdentity;
  private final SpecRpcServer specRpcServer;

  public OneHopServer(String serverIdentity) {
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
    OneHopServiceHostFactory oneHopHostFactory = new OneHopServiceHostFactory();
    specRpcServer.register(OneHopServiceHost.TEST_ONLY_ACTUAL_RETURN, oneHopHostFactory, String.class, String.class);
    specRpcServer.register(OneHopServiceHost.TEST_CORRECT_SPEC_RETURN, oneHopHostFactory, String.class, String.class);
    specRpcServer.register(OneHopServiceHost.TEST_INCORRECT_SPEC_RETURN, oneHopHostFactory, String.class, String.class);
    specRpcServer.register(OneHopServiceHost.TEST_BOTH_CORRECT_INCORRECT_SPEC_RETURN, oneHopHostFactory, String.class,
        String.class);
    specRpcServer.register(OneHopServiceHost.TEST_MULTIPLE_SPEC_RETURN, oneHopHostFactory, String.class, String.class);
    specRpcServer.register(OneHopServiceHost.TEST_EXCEPTION_RETURN, oneHopHostFactory, String.class, String.class);
    specRpcServer.register(OneHopServiceHost.TEST_EXCEPTION_AFTER_SPEC_RETURN, oneHopHostFactory, String.class,
        String.class);
  }

  public void run() {
    try {
      specRpcServer.execute();
    } catch (UninitializationException e) {
      e.printStackTrace();
    } catch (InterruptedException e) {
      e.printStackTrace();
    } catch (IOException e) {
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
}
