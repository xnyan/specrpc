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

package specrpc.onehop.iterative;

import java.io.IOException;

import rpc.execption.UninitializationException;
import specrpc.common.RpcSignature;
import specrpc.server.api.SpecRpcServer;

public class IterServer extends Thread {

  private final String serverIdentity;
  private final SpecRpcServer specRpcServer;

  public IterServer(String serverIdentity) {
    this.serverIdentity = serverIdentity;
    this.specRpcServer = new SpecRpcServer();
    try {
      // start server-side SpecRPC framework
      this.specRpcServer.initServer(this.serverIdentity, null);
      registerRPCMethods(); // register RPC methods
    } catch (IOException e) {
      e.printStackTrace();
    } catch (UninitializationException e) {
      e.printStackTrace();
    }
  }

  private void registerRPCMethods() throws UninitializationException {
    IterServiceHostFactory oneHopIterServiceHostFactory = new IterServiceHostFactory();
    specRpcServer.register(IterServiceHost.TEST_RETURN_VALUE, oneHopIterServiceHostFactory, String.class, String.class);
    specRpcServer.register(IterServiceHost.TEST_INCORRECT_SPEC_RETURN_VALUE, oneHopIterServiceHostFactory, String.class,
        String.class);
    specRpcServer.register(IterServiceHost.TEST_CORRECT_SPEC_RETURN_VALUE, oneHopIterServiceHostFactory, String.class,
        String.class);
    specRpcServer.register(IterServiceHost.TEST_MULTI_INCORRECT_SPEC_RETURN_VALUE, oneHopIterServiceHostFactory,
        String.class, String.class);
    specRpcServer.register(IterServiceHost.TEST_MULTI_SPEC_RETURN_VALUE_WITH_CORRECT_SPEC, oneHopIterServiceHostFactory,
        String.class, String.class);
    specRpcServer.register(IterServiceHost.TEST_SPEC_BLOCK_RETURN_VALUE, oneHopIterServiceHostFactory, String.class,
        String.class);
    specRpcServer.register(IterServiceHost.TEST_SPEC_BLOCK_BEFORE_ANY_RETURN, oneHopIterServiceHostFactory,
        String.class, String.class);
    specRpcServer.register(IterServiceHost.TEST_SPEC_BLOCK_INCORRECT_SPEC_RETURN_VALUE, oneHopIterServiceHostFactory,
        String.class, String.class);
    specRpcServer.register(IterServiceHost.TEST_SPEC_BLOCK_CORRECT_SPEC_RETURN_VALUE, oneHopIterServiceHostFactory,
        String.class, String.class);
    specRpcServer.register(IterServiceHost.TEST_SPEC_BLOCK_MULTI_INCORRECT_SPEC_RETURN_VALUE,
        oneHopIterServiceHostFactory, String.class, String.class);
    specRpcServer.register(IterServiceHost.TEST_SPEC_BLOCK_MULTI_SPEC_RETURN_VALUE_WITH_CORRECT_SPEC,
        oneHopIterServiceHostFactory, String.class, String.class);
    specRpcServer.register(IterServiceHost.TEST_RETURN_EXCEPTION, oneHopIterServiceHostFactory, String.class,
        String.class);
    specRpcServer.register(IterServiceHost.TEST_RETURN_EXCEPTION_AFTER_SPEC_RETURN, oneHopIterServiceHostFactory,
        String.class, String.class);
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
      ;
    }
  }

  public static RpcSignature getTestReturnValue() {
    return new RpcSignature(IterServiceHost.class.getName(), IterServiceHost.TEST_RETURN_VALUE, String.class,
        String.class);
  }

  public static RpcSignature getTestIncorrectSpecReturnValue() {
    return new RpcSignature(IterServiceHost.class.getName(), IterServiceHost.TEST_INCORRECT_SPEC_RETURN_VALUE,
        String.class, String.class);
  }

  public static RpcSignature getTestCorrectSpecReturnValue() {
    return new RpcSignature(IterServiceHost.class.getName(), IterServiceHost.TEST_CORRECT_SPEC_RETURN_VALUE,
        String.class, String.class);
  }

  public static RpcSignature getTestMultiIncorrectSpecReturnValue() {
    return new RpcSignature(IterServiceHost.class.getName(), IterServiceHost.TEST_MULTI_INCORRECT_SPEC_RETURN_VALUE,
        String.class, String.class);
  }

  public static RpcSignature getTestMultiSpecReturnValueWithCorrectSpec() {
    return new RpcSignature(IterServiceHost.class.getName(),
        IterServiceHost.TEST_MULTI_SPEC_RETURN_VALUE_WITH_CORRECT_SPEC, String.class, String.class);
  }

  public static RpcSignature getTestSpecBlockReturnValue() {
    return new RpcSignature(IterServiceHost.class.getName(), IterServiceHost.TEST_SPEC_BLOCK_RETURN_VALUE,
        String.class, String.class);
  }

  public static RpcSignature getTestSpecBlockBeforeAnyReturn() {
    return new RpcSignature(IterServiceHost.class.getName(), IterServiceHost.TEST_SPEC_BLOCK_BEFORE_ANY_RETURN,
        String.class, String.class);
  }

  public static RpcSignature getTestSpecBlockIncorrectSpecReturnValue() {
    return new RpcSignature(IterServiceHost.class.getName(),
        IterServiceHost.TEST_SPEC_BLOCK_INCORRECT_SPEC_RETURN_VALUE, String.class, String.class);
  }

  public static RpcSignature getTestSpecBlockCorrectSpecReturnValue() {
    return new RpcSignature(IterServiceHost.class.getName(),
        IterServiceHost.TEST_SPEC_BLOCK_CORRECT_SPEC_RETURN_VALUE, String.class, String.class);
  }

  public static RpcSignature getTestSpecBlockMultiIncorrectSpecReturnValue() {
    return new RpcSignature(IterServiceHost.class.getName(),
        IterServiceHost.TEST_SPEC_BLOCK_MULTI_INCORRECT_SPEC_RETURN_VALUE, String.class, String.class);
  }

  public static RpcSignature getTestSpecBlockMultiSpecReturnValueWithCorrectSpec() {
    return new RpcSignature(IterServiceHost.class.getName(),
        IterServiceHost.TEST_SPEC_BLOCK_MULTI_SPEC_RETURN_VALUE_WITH_CORRECT_SPEC, String.class, String.class);
  }

  public static RpcSignature getTestReturnException() {
    return new RpcSignature(IterServiceHost.class.getName(), IterServiceHost.TEST_RETURN_EXCEPTION, String.class,
        String.class);
  }

  public static RpcSignature getTestReturnExceptionAfterSpecReturn() {
    return new RpcSignature(IterServiceHost.class.getName(), IterServiceHost.TEST_RETURN_EXCEPTION_AFTER_SPEC_RETURN,
        String.class, String.class);
  }
}
