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
import java.util.ArrayList;
//import java.util.concurrent.BrokenBarrierException;

import Waterloo.MultiSocket.exception.ConnectionCloseException;
import Waterloo.MultiSocket.exception.MultiSocketValidException;

import junit.framework.Assert;

import rpc.execption.MethodNotRegisteredException;
import rpc.execption.NoClientStubException;
import rpc.execption.UserException;
import specrpc.client.api.SpecRpcCallbackFactory;
import specrpc.client.api.SpecRpcFuture;
import specrpc.client.api.SpecRpcClientStub;
import specrpc.common.RpcSignature;
import specrpc.exception.SpeculationFailException;
import specrpc.server.api.SpecRpcHostObject;

/*
 * ChainServiceHost provides the actual RPC for chain testing of SpecRPC
 * 
 * ChianServiceHost serves both the Middle Server and the End Server in
 * the two-hop chain.
 * 
 * Middle Server Testing Cases (Methods)
 * (1) return the value in RPC method after getting the response of next RPC
 * (2) return the value in Callback after getting the response of next RPC
 * (3) return the value in RPC method after a speculative return in RPC method
 * (4) return the value in Callback after a speculative return in Callback
 * (5) return the value in RPC method after multiple speculative return in RPC method
 * (6) return the value in Callback after multiple speculative return in Callback
 * 
 * End Server Testing Cases (Methods):
 * (1) just return the value
 * (2) return the actual value after a speculative return
 * (3) return the actual value after multiple speculative return
 * (4) just return an exception
 * (5) return an exception after a speculative return
 * (6) return an exception after multiple speculative return 
 */

public class ChainServiceHost extends SpecRpcHostObject {

  // method names
  // end server methods
  public static final String TEST_RETURN_VALUE = "testReturnValue";
  public static final String TEST_SPEC_RETURN_VALUE = "testSpecReturnValue";
  public static final String TEST_MULTI_SPEC_RETURN_VALUE = "testMultiSpecReturnValue";
  public static final String TEST_RETURN_EXCEPTION = "testReturnException";
  public static final String TEST_SPEC_RETURN_BEFORE_EXCEPTION = "testSpecReturnBeforeException";
  public static final String TEST_MULTI_SPEC_RETURN_BEFORE_EXCEPTION = "testMultiSpecReturnBeforeException";

  // mid server methods
  public static final String TEST_CHAIN_RETURN_VALUE = "testChainReturnValue";
  public static final String TEST_CHAIN_SPEC_RETURN_VALUE = "testChainSpecReturnValue";
  public static final String TEST_CHAIN_MULTI_SPEC_RETURN_VALUE = "testChainMultiSpecReturnValue";
  public static final String TEST_CHAIN_RETURN_VALUE_BY_CALLBACK = "testChainReturnValueByCallback";
  public static final String TEST_CHAIN_SPEC_RETURN_BY_CALLBACK = "testChainSpecReturnByCallback";
  public static final String TEST_CHAIN_MULTI_SPEC_RETURN_BY_CALLBACK = "testChainMultiSpecReturnByCallback";
  public static final String TEST_CHAIN_CORRECT_RETURN_BY_CALLBACK = "testChainCorrectReturnByCallback";
  public static final String TEST_CHAIN_CORRECT_SPEC_RETURN_BY_CALLBACK = "testChainCorrectSpecReturnByCallback";
  public static final String TEST_CHAIN_CORRECT_MULTI_SPEC_RETURN_BY_CALLBACK = "testChainCorrectMultiSpecReturnByCallback";
  public static final String TEST_CHAIN_SPEC_RETURN_NOT_BY_CALLBACK = "testChainSpecReturnNotByCallback";
  public static final String TEST_CHAIN_MULTI_SPEC_RETURN_NOT_BY_CALLBACK = "testChainMultSpecReturnNotByCallback";
  public static final String TEST_CHAIN_SPEC_RETURN_BY_RPC_AND_CALLBACK = "testChainSpecReturnByRPCAndCallback";
  public static final String TEST_CHAIN_MULTI_SPEC_RETURN_BY_RPC_AND_CALLBACK = "testChainMultiSpecReturnByRPCAndCallback";
  public static final String TEST_CHAIN_SPEC_BLOCK_IN_CALLBACK = "testChainSpecBlockInChainCallback";
  public static final String TEST_CHAIN_END_RETURN_EXCEPTION_MID_RETURN_NOT_BY_CALLBACK = "testChainEndReturnExceptionMidReturnNotByCallback";
  public static final String TEST_CHAIN_END_SPEC_RETURN_BEFORE_EXCEPTION_MID_RETURN_NOT_BYCALLBACK = "testChainEndSpecReturnBeforeExceptionMidReturnNotByCallback";
  public static final String TEST_CHAIN_END_MULTI_SPEC_RETURN_BEFORE_EXCEPTION_MID_RETURN_NOT_BY_CALLBACK = "testChainEndMultiSpecReturnBeforeExceptionMidReturnNotByCallback";
  public static final String TEST_CHAIN_END_RETURN_EXCEPTION_BY_CALLBACK_MID_RETURN_BY_CALLBACK = "testChainEndReturnExceptionByCallbackMidReturnByCallback";
  public static final String TEST_CHAIN_END_SPEC_RETURN_BEFORE_EXCEPTION_MID_RETURN_BY_CALLBACK = "testChainEndSpecReturnBeforeExceptionMidReturnByCallback";
  public static final String TEST_CHAIN_END_MULTI_SPEC_RETURN_BEFORE_EXCEPTION_MID_RETURN_BY_CALLBACK = "testChainEndMultiSpecReturnBeforeExceptionMidReturnByCallback";
  public static final String TEST_CHAIN_MID_RETURN_EXCEPTION_BY_CALLBACK = "testChainMidReturnExceptionByCallback";
  public static final String TEST_CHAIN_MID_RETURN_EXCEPTION_BY_CALLBACK_WITH_CORRECT_SPEC = "testChainMidReturnExceptionByCallbackWithCorrectSpec";

  public static final int WORK_TIME = 5; // ms

  public ChainServiceHost() {

  }

  private void doWork() {
    try {
      Thread.sleep(ChainServiceHost.WORK_TIME);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  // end server method
  // return only the actual response
  public String testReturnValue(String requestValue) throws NoClientStubException, SpeculationFailException,
      InterruptedException, IOException, MultiSocketValidException, ConnectionCloseException {
    String result = ChainServiceMessages.END_SERVER_RESPONSE_PREFIX + requestValue;
    return result;
  }

  // return one spec response before returning the actual one
  public String testSpecReturnValue(String requestValue) throws NoClientStubException, SpeculationFailException,
      InterruptedException, IOException, MultiSocketValidException, ConnectionCloseException {
    this.specRPCFacade.specReturn(ChainServiceMessages.END_SERVER_SPEC_RESPONSE_PREFIX_1 + requestValue);

    this.doWork();

    return testReturnValue(requestValue);
  }

  // return multi spec responses before returning the actual one
  public String testMultiSpecReturnValue(String requestValue) throws NoClientStubException, SpeculationFailException,
      InterruptedException, IOException, MultiSocketValidException, ConnectionCloseException {
    this.specRPCFacade.specReturn(ChainServiceMessages.END_SERVER_SPEC_RESPONSE_PREFIX_1 + requestValue);
    this.specRPCFacade.specReturn(ChainServiceMessages.END_SERVER_SPEC_RESPONSE_PREFIX_2 + requestValue);

    this.doWork();

    this.specRPCFacade.specReturn(ChainServiceMessages.END_SERVER_SPEC_RESPONSE_PREFIX_3 + requestValue);
    this.specRPCFacade.specReturn(ChainServiceMessages.END_SERVER_SPEC_RESPONSE_PREFIX_4 + requestValue);

    String result = ChainServiceMessages.END_SERVER_RESPONSE_PREFIX + requestValue;

    return result;
  }

  // only return exception
  public String testReturnException(String requestValue) throws NoClientStubException, SpeculationFailException,
      InterruptedException, IOException, MultiSocketValidException, ConnectionCloseException {
    String result = ChainServiceMessages.END_SERVER_EXCEPTION_PREFIX + requestValue;
    this.specRPCFacade.throwNonSpecExceptionToClient(result);
    return result;
  }

  // return one spec response before returning exception
  public String testSpecReturnBeforeException(String requestValue) throws NoClientStubException,
      SpeculationFailException, InterruptedException, IOException, MultiSocketValidException, ConnectionCloseException {
    this.specRPCFacade.specReturn(ChainServiceMessages.END_SERVER_SPEC_RESPONSE_PREFIX_1 + requestValue);

    this.doWork();

    return testReturnException(requestValue);
  }

  // return multi spec responses before returning exception
  public String testMultiSpecReturnBeforeException(String requestValue) throws NoClientStubException,
      SpeculationFailException, InterruptedException, IOException, MultiSocketValidException, ConnectionCloseException {
    this.specRPCFacade.specReturn(ChainServiceMessages.END_SERVER_SPEC_RESPONSE_PREFIX_1 + requestValue);
    this.specRPCFacade.specReturn(ChainServiceMessages.END_SERVER_SPEC_RESPONSE_PREFIX_2 + requestValue);

    this.doWork();

    this.specRPCFacade.specReturn(ChainServiceMessages.END_SERVER_SPEC_RESPONSE_PREFIX_3 + requestValue);
    this.specRPCFacade.specReturn(ChainServiceMessages.END_SERVER_SPEC_RESPONSE_PREFIX_4 + requestValue);

    String result = ChainServiceMessages.END_SERVER_EXCEPTION_PREFIX + requestValue;
    this.specRPCFacade.throwNonSpecExceptionToClient(result);

    return result;
  }

  // TODO: application exception handling to notify host in the chains

  // middle server methods
  // This method returns response back to client.
  public String testChainReturnValue(String requestValue) {
    return returnNotByCallback(ChainServer.getTestReturnValue(), requestValue);
  }

  public String testChainSpecReturnValue(String requestValue) {
    try {
      this.specRPCFacade.specReturn(ChainServiceMessages.MID_SERVER_SPEC_RESPONSE_PREFIX_1 + requestValue);
    } catch (NoClientStubException e) {
      e.printStackTrace();
    } catch (SpeculationFailException e) {
      e.printStackTrace();
    } catch (InterruptedException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    } catch (MultiSocketValidException e) {
      e.printStackTrace();
    } catch (ConnectionCloseException e) {
      e.printStackTrace();
    }
    return returnNotByCallback(ChainServer.getTestSpecReturnValue(), requestValue);
  }

  public String testChainMultiSpecReturnValue(String requestValue) {
    try {
      this.specRPCFacade.specReturn(ChainServiceMessages.MID_SERVER_SPEC_RESPONSE_PREFIX_1 + requestValue);
      this.specRPCFacade.specReturn(ChainServiceMessages.MID_SERVER_SPEC_RESPONSE_PREFIX_2 + requestValue);

      this.doWork();

      this.specRPCFacade.specReturn(ChainServiceMessages.MID_SERVER_SPEC_RESPONSE_PREFIX_3 + requestValue);
      this.specRPCFacade.specReturn(ChainServiceMessages.MID_SERVER_SPEC_RESPONSE_PREFIX_4 + requestValue);
    } catch (NoClientStubException e) {
      e.printStackTrace();
    } catch (SpeculationFailException e) {
      e.printStackTrace();
    } catch (InterruptedException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    } catch (MultiSocketValidException e) {
      e.printStackTrace();
    } catch (ConnectionCloseException e) {
      e.printStackTrace();
    }
    return returnNotByCallback(ChainServer.getTestMultiSpecReturnValue(), requestValue);
  }

  // This method does not return response back to client.
  // Instead, the callback returns response back to client.
  public String testChainReturnValueByCallback(String requestValue) {
    return returnByCallback(ChainServer.getTestReturnValue(), new ChainServerReturnCallbackFactory(), requestValue);
  }

  public String testChainSpecReturnByCallback(String requestValue) {
    return returnByCallback(ChainServer.getTestSpecReturnValue(), new ChainServerSpecReturnCallbackFactory(),
        requestValue);
  }

  public String testChainMultiSpecReturnByCallback(String requestValue) {
    return returnByCallback(ChainServer.getTestMultiSpecReturnValue(), new ChainServerMultiSpecReturnCallbackFactory(),
        requestValue);
  }

  // middle server makes correct speculation
  // aims at testing the nonSpecReturn function of CallbackStub
  public String testChainCorrectReturnByCallback(String requestValue) {
    return returnByCallback(ChainServer.getTestMultiSpecReturnValue(), // getTestSpecReturnValue(),//
        new ChainServerReturnCallbackFactory(), requestValue, ChainServiceMessages.MID_SERVER_CORRECT_SPECULATION,
        ChainServiceMessages.MID_SERVER_INCORRECT_SPECULATION);
  }

  public String testChainCorrectSpecReturnByCallback(String requestValue) {
    return returnByCallback(ChainServer.getTestMultiSpecReturnValue(), // getTestSpecReturnValue(),//
        new ChainServerSpecReturnCallbackFactory(), requestValue, ChainServiceMessages.MID_SERVER_CORRECT_SPECULATION,
        ChainServiceMessages.MID_SERVER_INCORRECT_SPECULATION);
  }

  public String testChainCorrectMultiSpecReturnByCallback(String requestValue) {
    return returnByCallback(ChainServer.getTestMultiSpecReturnValue(), new ChainServerMultiSpecReturnCallbackFactory(),
        requestValue, ChainServiceMessages.MID_SERVER_CORRECT_SPECULATION,
        ChainServiceMessages.MID_SERVER_INCORRECT_SPECULATION);
  }

  // Middle server makes speculation both in RPC and Callback
  public String testChainSpecReturnNotByCallback(String requestValue) {
    try {
      this.specRPCFacade.specReturn(ChainServiceMessages.MID_SERVER_SPEC_RESPONSE_PREFIX_1 + requestValue);
    } catch (NoClientStubException e) {
      e.printStackTrace();
    } catch (SpeculationFailException e) {
      e.printStackTrace();
    } catch (InterruptedException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    } catch (MultiSocketValidException e) {
      e.printStackTrace();
    } catch (ConnectionCloseException e) {
      e.printStackTrace();
    }

    return returnByCallback(ChainServer.getTestMultiSpecReturnValue(), new ChainServerReturnCallbackFactory(),
        requestValue, ChainServiceMessages.MID_SERVER_CORRECT_SPECULATION,
        ChainServiceMessages.MID_SERVER_INCORRECT_SPECULATION);
  }

  public String testChainMultSpecReturnNotByCallback(String requestValue) {
    try {
      this.specRPCFacade.specReturn(ChainServiceMessages.MID_SERVER_SPEC_RESPONSE_PREFIX_1 + requestValue);
      this.specRPCFacade.specReturn(ChainServiceMessages.MID_SERVER_SPEC_RESPONSE_PREFIX_2 + requestValue);
      this.doWork();
      this.specRPCFacade.specReturn(ChainServiceMessages.MID_SERVER_SPEC_RESPONSE_PREFIX_3 + requestValue);
      this.specRPCFacade.specReturn(ChainServiceMessages.MID_SERVER_SPEC_RESPONSE_PREFIX_4 + requestValue);
    } catch (NoClientStubException e) {
      e.printStackTrace();
    } catch (SpeculationFailException e) {
      e.printStackTrace();
    } catch (InterruptedException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    } catch (MultiSocketValidException e) {
      e.printStackTrace();
    } catch (ConnectionCloseException e) {
      e.printStackTrace();
    }

    return returnByCallback(ChainServer.getTestMultiSpecReturnValue(), new ChainServerReturnCallbackFactory(),
        requestValue, ChainServiceMessages.MID_SERVER_CORRECT_SPECULATION,
        ChainServiceMessages.MID_SERVER_INCORRECT_SPECULATION);
  }

  public String testChainSpecReturnByRPCAndCallback(String requestValue) {
    try {
      this.specRPCFacade.specReturn(ChainServiceMessages.MID_SERVER_SPEC_RESPONSE_PREFIX_1 + requestValue);
    } catch (NoClientStubException e) {
      e.printStackTrace();
    } catch (SpeculationFailException e) {
      e.printStackTrace();
    } catch (InterruptedException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    } catch (MultiSocketValidException e) {
      e.printStackTrace();
    } catch (ConnectionCloseException e) {
      e.printStackTrace();
    }

    return returnByCallback(ChainServer.getTestMultiSpecReturnValue(), new ChainServerSpecReturnCallbackFactory(),
        requestValue, ChainServiceMessages.MID_SERVER_CORRECT_SPECULATION,
        ChainServiceMessages.MID_SERVER_INCORRECT_SPECULATION);
  }

  public String testChainMultiSpecReturnByRPCAndCallback(String requestValue) {
    try {
      this.specRPCFacade.specReturn(ChainServiceMessages.MID_SERVER_SPEC_RESPONSE_PREFIX_1 + requestValue);
      this.specRPCFacade.specReturn(ChainServiceMessages.MID_SERVER_SPEC_RESPONSE_PREFIX_2 + requestValue);
      this.doWork();
      this.specRPCFacade.specReturn(ChainServiceMessages.MID_SERVER_SPEC_RESPONSE_PREFIX_3 + requestValue);
      this.specRPCFacade.specReturn(ChainServiceMessages.MID_SERVER_SPEC_RESPONSE_PREFIX_4 + requestValue);
    } catch (NoClientStubException e) {
      e.printStackTrace();
    } catch (SpeculationFailException e) {
      e.printStackTrace();
    } catch (InterruptedException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    } catch (MultiSocketValidException e) {
      e.printStackTrace();
    } catch (ConnectionCloseException e) {
      e.printStackTrace();
    }

    return returnByCallback(ChainServer.getTestMultiSpecReturnValue(), new ChainServerMultiSpecReturnCallbackFactory(),
        requestValue, ChainServiceMessages.MID_SERVER_CORRECT_SPECULATION,
        ChainServiceMessages.MID_SERVER_INCORRECT_SPECULATION);
  }

  // test specBlock() in Callback
  public String testChainSpecBlockInChainCallback(String requestValue) {
    ChainServerSpecBlockCallbackFactory factory = new ChainServerSpecBlockCallbackFactory();
    factory.setBarrier(6);
    String result = returnByCallback(ChainServer.getTestMultiSpecReturnValue(), factory, requestValue,
        ChainServiceMessages.MID_SERVER_CORRECT_SPECULATION, ChainServiceMessages.MID_SERVER_INCORRECT_SPECULATION);

    Assert.assertEquals(6, factory.getCallbackCount());
    Assert.assertEquals(5, factory.getFailSpecBlockNum());
    Assert.assertEquals(1, factory.getPassSpecBlockNum());
    return result;
  }

  // test return exceptions
  public String testChainEndReturnExceptionMidReturnNotByCallback(String requestValue) {
    return returnNotByCallback(ChainServer.getTestReturnException(), requestValue,
        ChainServiceMessages.MID_SERVER_CORRECT_SPECULATION, ChainServiceMessages.MID_SERVER_INCORRECT_SPECULATION);
  }

  public String testChainEndSpecReturnBeforeExceptionMidReturnNotByCallback(String requestValue) {
    return returnNotByCallback(ChainServer.getTestSpecReturnBeforeException(), requestValue,
        ChainServiceMessages.MID_SERVER_CORRECT_SPECULATION, ChainServiceMessages.MID_SERVER_INCORRECT_SPECULATION);
  }

  public String testChainEndMultiSpecReturnBeforeExceptionMidReturnNotByCallback(String requestValue) {
    return returnNotByCallback(ChainServer.getTestMultiSpecReturnBeforeException(), requestValue,
        ChainServiceMessages.MID_SERVER_CORRECT_SPECULATION, ChainServiceMessages.MID_SERVER_INCORRECT_SPECULATION);
  }

  public String testChainEndReturnExceptionByCallbackMidReturnByCallback(String requestValue) {
    return returnExceptionReturnValueByCallback(ChainServer.getTestReturnException(),
        new ChainServerReturnCallbackFactory(), requestValue, ChainServiceMessages.MID_SERVER_CORRECT_SPECULATION,
        ChainServiceMessages.MID_SERVER_INCORRECT_SPECULATION);
  }

  public String testChainEndSpecReturnBeforeExceptionMidReturnByCallback(String requestValue) {
    return returnExceptionReturnValueByCallback(ChainServer.getTestSpecReturnBeforeException(),
        new ChainServerSpecReturnCallbackFactory(), requestValue, ChainServiceMessages.MID_SERVER_CORRECT_SPECULATION,
        ChainServiceMessages.MID_SERVER_INCORRECT_SPECULATION);
  }

  public String testChainEndMultiSpecReturnBeforeExceptionMidReturnByCallback(String requestValue) {
    return returnExceptionReturnValueByCallback(ChainServer.getTestMultiSpecReturnBeforeException(),
        new ChainServerMultiSpecReturnCallbackFactory(), requestValue,
        ChainServiceMessages.MID_SERVER_CORRECT_SPECULATION, ChainServiceMessages.MID_SERVER_INCORRECT_SPECULATION);
  }

  public String testChainMidReturnExceptionByCallback(String requestValue) {
    ChainServerExceptionCallbackFactory factory = new ChainServerExceptionCallbackFactory();
    factory.setBarrier(6);
    String result = returnByCallback(ChainServer.getTestMultiSpecReturnValue(), factory, requestValue,
        ChainServiceMessages.MID_SERVER_INCORRECT_SPECULATION);

    Assert.assertEquals(6, factory.getCallbackCount());
    Assert.assertEquals(5, factory.getFailSpecBlockNum());
    Assert.assertEquals(1, factory.getPassSpecBlockNum());

    return result;
  }

  public String testChainMidReturnExceptionByCallbackWithCorrectSpec(String requestValue) {
    ChainServerExceptionCallbackFactory factory = new ChainServerExceptionCallbackFactory();
    factory.setBarrier(6);
    String result = returnByCallback(ChainServer.getTestMultiSpecReturnValue(), factory, requestValue,
        ChainServiceMessages.MID_SERVER_CORRECT_SPECULATION, ChainServiceMessages.MID_SERVER_INCORRECT_SPECULATION);

    Assert.assertEquals(6, factory.getCallbackCount());
    Assert.assertEquals(5, factory.getFailSpecBlockNum());
    Assert.assertEquals(1, factory.getPassSpecBlockNum());

    return result;
  }

  private String returnNotByCallback(RpcSignature rpcMethod, String requestValue, Object... predictedValues) {
    String result = null;
    try {
      SpecRpcClientStub srvStub = this.specRPCFacade.bind(ChainTest.SERVER_IDENTITY, rpcMethod);
      ArrayList<Object> predictions = null;
      if (predictedValues != null) {
        predictions = new ArrayList<Object>();
        for (Object value : predictedValues) {
          predictions.add(value);
        }
      }

      SpecRpcFuture future = srvStub.call(predictions, new ChainServerCallbackFactory(), requestValue);

      result = ChainServiceMessages.MID_SERVER_RESPONSE_PREFIX + future.getResult();

    } catch (SpeculationFailException e) {
      e.printStackTrace();
    } catch (UserException e) {
      try {
        this.specRPCFacade.throwNonSpecExceptionToClient(e.getMessage());
      } catch (NoClientStubException e1) {
        e1.printStackTrace();
      } catch (SpeculationFailException e1) {
        e1.printStackTrace();
      } catch (InterruptedException e1) {
        e1.printStackTrace();
      } catch (IOException e1) {
        e1.printStackTrace();
      } catch (MultiSocketValidException e1) {
        e1.printStackTrace();
      } catch (ConnectionCloseException e1) {
        e1.printStackTrace();
      }
    } catch (MethodNotRegisteredException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    return result;
  }

  private String returnByCallback(RpcSignature rpcMethod, SpecRpcCallbackFactory callbackFactory,
      String requestValue, Object... predictedValues) {
    String result = null;

    try {
      SpecRpcClientStub srvStub = this.specRPCFacade.bind(ChainTest.SERVER_IDENTITY, rpcMethod);
      ArrayList<Object> predictions = null;
      if (predictedValues != null) {
        predictions = new ArrayList<Object>();
        for (Object value : predictedValues) {
          predictions.add(value);
        }
      }
      result = srvStub.call(predictions, callbackFactory, requestValue).getResult() + "";
    } catch (SpeculationFailException e) {
      e.printStackTrace();
    } catch (MethodNotRegisteredException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    } catch (InterruptedException e) {
      e.printStackTrace();
    } catch (UserException e) {
      e.printStackTrace();
    }

    return result;
  }

  private String returnExceptionReturnValueByCallback(RpcSignature rpcMethod, SpecRpcCallbackFactory callbackFactory,
      String requestValue, Object... predictedValues) {
    String result = null;

    try {
      SpecRpcClientStub srvStub = this.specRPCFacade.bind(ChainTest.SERVER_IDENTITY, rpcMethod);
      ArrayList<Object> predictions = null;
      if (predictedValues != null) {
        predictions = new ArrayList<Object>();
        for (Object value : predictedValues) {
          predictions.add(value);
        }
      }
      SpecRpcFuture future = srvStub.call(predictions, callbackFactory, requestValue);
      // It is necessary to block on getting the result of callback
      // that is, allow the callback finish returning the value to client
      // after this RPC method finishes socket will be closed but the callback
      // is running and will use the socket to return value to client
      result = (String) future.getResult();

    } catch (SpeculationFailException e) {
      e.printStackTrace();
    } catch (UserException e) {
      try {
        this.specRPCFacade.throwNonSpecExceptionToClient(e.getMessage());
      } catch (NoClientStubException e1) {
        e1.printStackTrace();
      } catch (SpeculationFailException e1) {
        e1.printStackTrace();
      } catch (InterruptedException e1) {
        e1.printStackTrace();
      } catch (IOException e1) {
        e1.printStackTrace();
      } catch (MultiSocketValidException e1) {
        e1.printStackTrace();
      } catch (ConnectionCloseException e1) {
        e1.printStackTrace();
      }
    } catch (MethodNotRegisteredException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    return result;
  }

}
