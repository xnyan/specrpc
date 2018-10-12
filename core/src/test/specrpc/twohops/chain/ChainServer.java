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

package specrpc.twohops.chain;

import java.io.IOException;

import rpc.execption.UninitializationException;
import specrpc.common.RpcSignature;
import specrpc.server.api.SpecRpcServer;

public class ChainServer extends Thread {

  private final String serverIdentity;
  private final SpecRpcServer specRpcServer;

  public ChainServer(String serverIdentity) {
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
    ChainServiceHostFactory chainHostFactory = new ChainServiceHostFactory();

    // end server methods
    specRpcServer.register(ChainServiceHost.TEST_RETURN_VALUE, chainHostFactory, String.class, String.class);
    specRpcServer.register(ChainServiceHost.TEST_SPEC_RETURN_VALUE, chainHostFactory, String.class, String.class);
    specRpcServer.register(ChainServiceHost.TEST_MULTI_SPEC_RETURN_VALUE, chainHostFactory, String.class, String.class);
    specRpcServer.register(ChainServiceHost.TEST_RETURN_EXCEPTION, chainHostFactory, String.class, String.class);
    specRpcServer.register(ChainServiceHost.TEST_SPEC_RETURN_BEFORE_EXCEPTION, chainHostFactory, String.class,
        String.class);
    specRpcServer.register(ChainServiceHost.TEST_MULTI_SPEC_RETURN_BEFORE_EXCEPTION, chainHostFactory, String.class,
        String.class);

    // middle server methods
    specRpcServer.register(ChainServiceHost.TEST_CHAIN_RETURN_VALUE, chainHostFactory, String.class, String.class);
    specRpcServer.register(ChainServiceHost.TEST_CHAIN_SPEC_RETURN_VALUE, chainHostFactory, String.class, String.class);
    specRpcServer.register(ChainServiceHost.TEST_CHAIN_MULTI_SPEC_RETURN_VALUE, chainHostFactory, String.class,
        String.class);
    specRpcServer.register(ChainServiceHost.TEST_CHAIN_RETURN_VALUE_BY_CALLBACK, chainHostFactory, String.class,
        String.class);
    specRpcServer.register(ChainServiceHost.TEST_CHAIN_SPEC_RETURN_BY_CALLBACK, chainHostFactory, String.class,
        String.class);
    specRpcServer.register(ChainServiceHost.TEST_CHAIN_MULTI_SPEC_RETURN_BY_CALLBACK, chainHostFactory, String.class,
        String.class);
    specRpcServer.register(ChainServiceHost.TEST_CHAIN_CORRECT_RETURN_BY_CALLBACK, chainHostFactory, String.class,
        String.class);
    specRpcServer.register(ChainServiceHost.TEST_CHAIN_CORRECT_SPEC_RETURN_BY_CALLBACK, chainHostFactory, String.class,
        String.class);
    specRpcServer.register(ChainServiceHost.TEST_CHAIN_CORRECT_MULTI_SPEC_RETURN_BY_CALLBACK, chainHostFactory,
        String.class, String.class);
    specRpcServer.register(ChainServiceHost.TEST_CHAIN_SPEC_RETURN_NOT_BY_CALLBACK, chainHostFactory, String.class,
        String.class);
    specRpcServer.register(ChainServiceHost.TEST_CHAIN_MULTI_SPEC_RETURN_NOT_BY_CALLBACK, chainHostFactory,
        String.class, String.class);
    specRpcServer.register(ChainServiceHost.TEST_CHAIN_SPEC_RETURN_BY_RPC_AND_CALLBACK, chainHostFactory, String.class,
        String.class);
    specRpcServer.register(ChainServiceHost.TEST_CHAIN_MULTI_SPEC_RETURN_BY_RPC_AND_CALLBACK, chainHostFactory,
        String.class, String.class);
    specRpcServer.register(ChainServiceHost.TEST_CHAIN_SPEC_BLOCK_IN_CALLBACK, chainHostFactory, String.class,
        String.class);
    specRpcServer.register(ChainServiceHost.TEST_CHAIN_END_RETURN_EXCEPTION_MID_RETURN_NOT_BY_CALLBACK,
        chainHostFactory, String.class, String.class);
    specRpcServer.register(ChainServiceHost.TEST_CHAIN_END_SPEC_RETURN_BEFORE_EXCEPTION_MID_RETURN_NOT_BYCALLBACK,
        chainHostFactory, String.class, String.class);
    specRpcServer.register(
        ChainServiceHost.TEST_CHAIN_END_MULTI_SPEC_RETURN_BEFORE_EXCEPTION_MID_RETURN_NOT_BY_CALLBACK, chainHostFactory,
        String.class, String.class);
    specRpcServer.register(ChainServiceHost.TEST_CHAIN_END_RETURN_EXCEPTION_BY_CALLBACK_MID_RETURN_BY_CALLBACK,
        chainHostFactory, String.class, String.class);
    specRpcServer.register(ChainServiceHost.TEST_CHAIN_END_SPEC_RETURN_BEFORE_EXCEPTION_MID_RETURN_BY_CALLBACK,
        chainHostFactory, String.class, String.class);
    specRpcServer.register(ChainServiceHost.TEST_CHAIN_END_MULTI_SPEC_RETURN_BEFORE_EXCEPTION_MID_RETURN_BY_CALLBACK,
        chainHostFactory, String.class, String.class);
    specRpcServer.register(ChainServiceHost.TEST_CHAIN_MID_RETURN_EXCEPTION_BY_CALLBACK, chainHostFactory, String.class,
        String.class);
    specRpcServer.register(ChainServiceHost.TEST_CHAIN_MID_RETURN_EXCEPTION_BY_CALLBACK_WITH_CORRECT_SPEC,
        chainHostFactory, String.class, String.class);
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

  // end server methods
  public static RpcSignature getTestReturnValue() {
    return new RpcSignature(ChainServiceHost.class.getName(), ChainServiceHost.TEST_RETURN_VALUE, String.class,
        String.class);
  }

  public static RpcSignature getTestSpecReturnValue() {
    return new RpcSignature(ChainServiceHost.class.getName(), ChainServiceHost.TEST_SPEC_RETURN_VALUE, String.class,
        String.class);
  }

  public static RpcSignature getTestMultiSpecReturnValue() {
    return new RpcSignature(ChainServiceHost.class.getName(), ChainServiceHost.TEST_MULTI_SPEC_RETURN_VALUE,
        String.class, String.class);
  }

  public static RpcSignature getTestReturnException() {
    return new RpcSignature(ChainServiceHost.class.getName(), ChainServiceHost.TEST_RETURN_EXCEPTION, String.class,
        String.class);
  }

  public static RpcSignature getTestSpecReturnBeforeException() {
    return new RpcSignature(ChainServiceHost.class.getName(), ChainServiceHost.TEST_SPEC_RETURN_BEFORE_EXCEPTION,
        String.class, String.class);
  }

  public static RpcSignature getTestMultiSpecReturnBeforeException() {
    return new RpcSignature(ChainServiceHost.class.getName(),
        ChainServiceHost.TEST_MULTI_SPEC_RETURN_BEFORE_EXCEPTION, String.class, String.class);
  }

  // mid server methods
  public static RpcSignature getTestChainReturnValue() {
    return new RpcSignature(ChainServiceHost.class.getName(), ChainServiceHost.TEST_CHAIN_RETURN_VALUE, String.class,
        String.class);
  }

  public static RpcSignature getTestChainSpecReturnValue() {
    return new RpcSignature(ChainServiceHost.class.getName(), ChainServiceHost.TEST_CHAIN_SPEC_RETURN_VALUE,
        String.class, String.class);
  }

  public static RpcSignature getTestChainMultiSpecReturnValue() {
    return new RpcSignature(ChainServiceHost.class.getName(), ChainServiceHost.TEST_CHAIN_MULTI_SPEC_RETURN_VALUE,
        String.class, String.class);
  }

  public static RpcSignature getTestChainReturnValueByCallback() {
    return new RpcSignature(ChainServiceHost.class.getName(), ChainServiceHost.TEST_CHAIN_RETURN_VALUE_BY_CALLBACK,
        String.class, String.class);
  }

  public static RpcSignature getTestChainSpecReturnByCallback() {
    return new RpcSignature(ChainServiceHost.class.getName(), ChainServiceHost.TEST_CHAIN_SPEC_RETURN_BY_CALLBACK,
        String.class, String.class);
  }

  public static RpcSignature getTestChainMultiSpecReturnByCallback() {
    return new RpcSignature(ChainServiceHost.class.getName(),
        ChainServiceHost.TEST_CHAIN_MULTI_SPEC_RETURN_BY_CALLBACK, String.class, String.class);
  }

  public static RpcSignature getTestChainCorrectReturnByCallback() {
    return new RpcSignature(ChainServiceHost.class.getName(), ChainServiceHost.TEST_CHAIN_CORRECT_RETURN_BY_CALLBACK,
        String.class, String.class);
  }

  public static RpcSignature getTestChainCorrectSpecReturnByCallback() {
    return new RpcSignature(ChainServiceHost.class.getName(),
        ChainServiceHost.TEST_CHAIN_CORRECT_SPEC_RETURN_BY_CALLBACK, String.class, String.class);
  }

  public static RpcSignature getTestChainCorrectMultiSpecReturnByCallback() {
    return new RpcSignature(ChainServiceHost.class.getName(),
        ChainServiceHost.TEST_CHAIN_CORRECT_MULTI_SPEC_RETURN_BY_CALLBACK, String.class, String.class);
  }
}