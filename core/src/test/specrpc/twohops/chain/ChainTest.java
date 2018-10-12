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

import static org.junit.Assert.*;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Test;
import org.junit.Before;
import org.junit.After;

import rpc.execption.UserException;
import specrpc.client.api.SpecRpcCallbackFactory;
import specrpc.client.api.SpecRpcStatistics;

/*
 * Two-hop chain: Client-->MidServer-->EndServer
 * MidServer & EndServer are the same server thread
 */
public class ChainTest {

  public static final String SERVER_IDENTITY = "ChainTestServer-ID";

  private static ChainServer server = new ChainServer(ChainTest.SERVER_IDENTITY);
  private static ChainClient client = new ChainClient();
  private static boolean serverStarted = false;
  private final String expectedResult = ChainClient.CORRECT_SPEC_VALUE;
  private final String expectedEndServerException = ChainServiceMessages.END_SERVER_EXCEPTION_PREFIX
      + ChainClient.REQUEST_VALUE;
  private final String expectedMidServerException = ChainServiceMessages.MID_SERVER_EXCEPTION_PREFIX
      + ChainServiceMessages.MID_SERVER_CORRECT_SPECULATION;

  @Before
  public void beforeEach() {
    if (serverStarted == false) {
      server.start();
      serverStarted = true;
    }
  }

  @After
  public void afterEach() {
    // server.terminate();
    try {
      // allow incorrectly speculative callback to finish before Server/Client
      // terminate
      // otherwise exception may happen in multi spec return from server because
      // the shutdown of client thread pool terminates kills the incorrectly
      // speculative callback thread(s)
      Thread.sleep(200);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    if (SpecRpcStatistics.isEnabled) {
      if (SpecRpcStatistics.isCountingIncorrectPrediction) {
        Assert.assertEquals(SpecRpcStatistics.getTotalPredictionNumber(),
            SpecRpcStatistics.getCorrectPredictionNumber() + SpecRpcStatistics.getIncorrectPredictionNumber());
      }
    }
  }

  // Use Debug Mode to test whether all threads all finished
  // that is, there is no deadlock (especially for the callback
  // that are based on incorrect speculation)
  @AfterClass
  public static void afterAll() throws InterruptedException {
    // allow thread pool to automatically kill idle threads
    // Thread.sleep(60 * 1000);

    // add break point here to check the threads
    server.terminate();
    client.terminate();

    if (SpecRpcStatistics.isEnabled) {
      if (SpecRpcStatistics.isCountingIncorrectPrediction) {
        Assert.assertEquals(SpecRpcStatistics.getTotalPredictionNumber(),
            SpecRpcStatistics.getCorrectPredictionNumber() + SpecRpcStatistics.getIncorrectPredictionNumber());
      }
    }
  }

  // each server only returns the actual value in RPC method
  @Test(timeout = 1000)
  public void testChainActualReturn() throws UserException {
    // assert result
    assertEquals(expectedResult, client.testReturnValue(ChainTest.SERVER_IDENTITY,
        ChainServiceHost.TEST_CHAIN_RETURN_VALUE, ChainClient.REQUEST_VALUE));
  }

  // each server returns one spec value in RPC method
  @Test(timeout = 1000)
  public void testChainSpecReturn() throws UserException {
    // assert result
    assertEquals(expectedResult, client.testReturnValue(ChainTest.SERVER_IDENTITY,
        ChainServiceHost.TEST_CHAIN_SPEC_RETURN_VALUE, ChainClient.REQUEST_VALUE));
  }

  // each server returns multi spec values in RPC method
  @Test(timeout = 1000)
  public void testChainMultiSpecReturn() throws UserException {
    // assert result
    assertEquals(expectedResult, client.testReturnValue(ChainTest.SERVER_IDENTITY,
        ChainServiceHost.TEST_CHAIN_MULTI_SPEC_RETURN_VALUE, ChainClient.REQUEST_VALUE));
  }

  // middle server actually returns in callback
  @Test(timeout = 1000)
  public void testChainCallbackActualReturn() throws UserException {
    // assert result
    assertEquals(expectedResult, client.testReturnValue(ChainTest.SERVER_IDENTITY,
        ChainServiceHost.TEST_CHAIN_RETURN_VALUE_BY_CALLBACK, ChainClient.REQUEST_VALUE));
  }

  // middle server returns one spec value in callback
  @Test(timeout = 1000)
  public void testChainCallbackSpecReturn() throws UserException {
    // assert result
    assertEquals(expectedResult, client.testReturnValue(ChainTest.SERVER_IDENTITY,
        ChainServiceHost.TEST_CHAIN_SPEC_RETURN_BY_CALLBACK, ChainClient.REQUEST_VALUE));
  }

  // middle server returns multi spec values in callback
  @Test(timeout = 1000)
  public void testChainCallbackMultiSpecReturn() throws UserException {
    // assert result
    assertEquals(expectedResult, client.testReturnValue(ChainTest.SERVER_IDENTITY,
        ChainServiceHost.TEST_CHAIN_MULTI_SPEC_RETURN_BY_CALLBACK, ChainClient.REQUEST_VALUE));
  }

  // middle server correctly speculate the response of end server
  // these 3 test cases aim at testing the nonSpecReturn of CallbackStub
  @Test(timeout = 1000)
  public void testChainCorrectReturnByCallback() throws UserException {
    // assert result
    assertEquals(expectedResult, client.testReturnValue(ChainTest.SERVER_IDENTITY,
        ChainServiceHost.TEST_CHAIN_CORRECT_RETURN_BY_CALLBACK, ChainClient.REQUEST_VALUE));
  }

  @Test(timeout = 1000)
  public void testChainCorrectSpecReturnByCallback() throws UserException {
    // assert result
    assertEquals(expectedResult, client.testReturnValue(ChainTest.SERVER_IDENTITY,
        ChainServiceHost.TEST_CHAIN_CORRECT_SPEC_RETURN_BY_CALLBACK, ChainClient.REQUEST_VALUE));
  }

  @Test(timeout = 1000)
  public void testChainCorrectMultiSpecReturnByCallback() throws UserException {
    // assert result
    assertEquals(expectedResult, client.testReturnValue(ChainTest.SERVER_IDENTITY,
        ChainServiceHost.TEST_CHAIN_CORRECT_MULTI_SPEC_RETURN_BY_CALLBACK, ChainClient.REQUEST_VALUE));
  }

  // client speculates correctly
  @Test(timeout = 1000)
  public void testClientCorrectSpecChainCorrectReturnByCallback() throws UserException {
    // assert result
    assertEquals(expectedResult,
        client.testReturnValue(ChainTest.SERVER_IDENTITY, ChainServiceHost.TEST_CHAIN_CORRECT_RETURN_BY_CALLBACK,
            ChainClient.REQUEST_VALUE, ChainClient.CORRECT_SPEC_VALUE, ChainClient.INCORRECT_SPEC_VALUE));
  }

  @Test(timeout = 1000)
  public void testClientCorrectSpecChainCorrectSpecReturnByCallback() throws UserException {
    // assert result
    assertEquals(expectedResult,
        client.testReturnValue(ChainTest.SERVER_IDENTITY, ChainServiceHost.TEST_CHAIN_CORRECT_SPEC_RETURN_BY_CALLBACK,
            ChainClient.REQUEST_VALUE, ChainClient.CORRECT_SPEC_VALUE, ChainClient.INCORRECT_SPEC_VALUE));
  }

  @Test(timeout = 1000)
  public void testClientCorrectSpecChainCorrectMultiSpecReturnByCallback() throws UserException {
    // assert result
    assertEquals(expectedResult,
        client.testReturnValue(ChainTest.SERVER_IDENTITY,
            ChainServiceHost.TEST_CHAIN_CORRECT_MULTI_SPEC_RETURN_BY_CALLBACK, ChainClient.REQUEST_VALUE,
            ChainClient.CORRECT_SPEC_VALUE, ChainClient.INCORRECT_SPEC_VALUE));
  }

  @Test(timeout = 1000)
  public void testChainSpecReturnNotByCallback() throws UserException {
    // assert result
    assertEquals(expectedResult,
        client.testReturnValue(ChainTest.SERVER_IDENTITY, ChainServiceHost.TEST_CHAIN_SPEC_RETURN_NOT_BY_CALLBACK,
            ChainClient.REQUEST_VALUE, ChainClient.CORRECT_SPEC_VALUE, ChainClient.INCORRECT_SPEC_VALUE));
  }

  @Test(timeout = 1000)
  public void testChainMultSpecReturnNotByCallback() throws UserException {
    // assert result
    assertEquals(expectedResult,
        client.testReturnValue(ChainTest.SERVER_IDENTITY, ChainServiceHost.TEST_CHAIN_MULTI_SPEC_RETURN_NOT_BY_CALLBACK,
            ChainClient.REQUEST_VALUE, ChainClient.CORRECT_SPEC_VALUE, ChainClient.INCORRECT_SPEC_VALUE));
  }

  @Test(timeout = 1000)
  public void testChainSpecReturnByRPCAndCallback() throws UserException {
    // assert result
    assertEquals(expectedResult,
        client.testReturnValue(ChainTest.SERVER_IDENTITY, ChainServiceHost.TEST_CHAIN_SPEC_RETURN_BY_RPC_AND_CALLBACK,
            ChainClient.REQUEST_VALUE, ChainClient.CORRECT_SPEC_VALUE, ChainClient.INCORRECT_SPEC_VALUE));
  }

  @Test(timeout = 1000)
  public void testChainMultiSpecReturnByRPCAndCallback() throws UserException {
    // assert result
    assertEquals(expectedResult,
        client.testReturnValue(ChainTest.SERVER_IDENTITY,
            ChainServiceHost.TEST_CHAIN_MULTI_SPEC_RETURN_BY_RPC_AND_CALLBACK, ChainClient.REQUEST_VALUE,
            ChainClient.CORRECT_SPEC_VALUE, ChainClient.INCORRECT_SPEC_VALUE));
  }

  // test specBlock()
  @Test(timeout = 1000)
  public void testChainSpecBlockInChainCallback() throws UserException {
    assertEquals(expectedResult,
        client.testReturnValue(ChainTest.SERVER_IDENTITY, ChainServiceHost.TEST_CHAIN_SPEC_BLOCK_IN_CALLBACK,
            ChainClient.REQUEST_VALUE, ChainClient.CORRECT_SPEC_VALUE, ChainClient.INCORRECT_SPEC_VALUE));

  }

  @Test(timeout = 1000)
  public void testChainSpecBlockInClientCallback() throws UserException {
    SpecRpcCallbackFactory defaultFactory = client.getFactory();
    ChainClientSpecBlockCallbackFactory factory = new ChainClientSpecBlockCallbackFactory();
    factory.setBarrier(6);

    client.setFactory(factory);
    String result = client.testReturnValue(ChainTest.SERVER_IDENTITY,
        ChainServiceHost.TEST_CHAIN_MULTI_SPEC_RETURN_VALUE, ChainClient.REQUEST_VALUE, ChainClient.CORRECT_SPEC_VALUE,
        ChainClient.INCORRECT_SPEC_VALUE);

    client.setFactory(defaultFactory);

    // assert result
    assertEquals(expectedResult, result);
    assertEquals(6, factory.getCallbackCount());
    assertEquals(5, factory.getFailSpecBlockNum());
    assertEquals(1, factory.getPassSpecBlockNum());
  }

  // test return exceptions
  // exception from the end server
  @Test(timeout = 1000)
  public void testChainEndReturnExceptionMidReturnNotByCallback() {
    try {
      client.testReturnValue(ChainTest.SERVER_IDENTITY,
          ChainServiceHost.TEST_CHAIN_END_RETURN_EXCEPTION_MID_RETURN_NOT_BY_CALLBACK, ChainClient.REQUEST_VALUE,
          ChainClient.CORRECT_SPEC_VALUE, ChainClient.INCORRECT_SPEC_VALUE);
    } catch (UserException e) {
      assertEquals(expectedEndServerException, e.getMessage());
    }
  }

  @Test(timeout = 1000)
  public void testChainEndSpecReturnBeforeExceptionMidReturnNotByCallback() {
    try {
      client.testReturnValue(ChainTest.SERVER_IDENTITY,
          ChainServiceHost.TEST_CHAIN_END_SPEC_RETURN_BEFORE_EXCEPTION_MID_RETURN_NOT_BYCALLBACK,
          ChainClient.REQUEST_VALUE, ChainClient.CORRECT_SPEC_VALUE, ChainClient.INCORRECT_SPEC_VALUE);
    } catch (UserException e) {
      assertEquals(expectedEndServerException, e.getMessage());
    }
  }

  @Test(timeout = 1000)
  public void testChainEndMultiSpecReturnBeforeExceptionMidReturnNotByCallback() {
    try {
      client.testReturnValue(ChainTest.SERVER_IDENTITY,
          ChainServiceHost.TEST_CHAIN_END_MULTI_SPEC_RETURN_BEFORE_EXCEPTION_MID_RETURN_NOT_BY_CALLBACK,
          ChainClient.REQUEST_VALUE, ChainClient.CORRECT_SPEC_VALUE, ChainClient.INCORRECT_SPEC_VALUE);
    } catch (UserException e) {
      assertEquals(expectedEndServerException, e.getMessage());
    }
  }

  @Test(timeout = 1000)
  public void testChainEndReturnExceptionByCallbackMidReturnByCallback() {
    try {
      client.testReturnValue(ChainTest.SERVER_IDENTITY,
          ChainServiceHost.TEST_CHAIN_END_RETURN_EXCEPTION_BY_CALLBACK_MID_RETURN_BY_CALLBACK,
          ChainClient.REQUEST_VALUE, ChainClient.CORRECT_SPEC_VALUE, ChainClient.INCORRECT_SPEC_VALUE);
    } catch (UserException e) {
      assertEquals(expectedEndServerException, e.getMessage());
    }
  }

  @Test(timeout = 1000)
  public void testChainEndSpecReturnBeforeExceptionMidReturnByCallback() {
    try {
      client.testReturnValue(ChainTest.SERVER_IDENTITY,
          ChainServiceHost.TEST_CHAIN_END_SPEC_RETURN_BEFORE_EXCEPTION_MID_RETURN_BY_CALLBACK,
          ChainClient.REQUEST_VALUE, ChainClient.CORRECT_SPEC_VALUE, ChainClient.INCORRECT_SPEC_VALUE);
    } catch (UserException e) {
      assertEquals(expectedEndServerException, e.getMessage());
    }
  }

  @Test(timeout = 1000)
  public void testChainEndMultiSpecReturnBeforeExceptionMidReturnByCallback() {
    try {
      client.testReturnValue(ChainTest.SERVER_IDENTITY,
          ChainServiceHost.TEST_CHAIN_END_MULTI_SPEC_RETURN_BEFORE_EXCEPTION_MID_RETURN_BY_CALLBACK,
          ChainClient.REQUEST_VALUE, ChainClient.CORRECT_SPEC_VALUE, ChainClient.INCORRECT_SPEC_VALUE);
    } catch (UserException e) {
      assertEquals(expectedEndServerException, e.getMessage());
    }
  }

  // exception from the Callback in mid server
  @Test(timeout = 1000)
  public void testChainMidReturnExceptionByCallback() {
    try {
      client.testReturnValue(ChainTest.SERVER_IDENTITY, ChainServiceHost.TEST_CHAIN_MID_RETURN_EXCEPTION_BY_CALLBACK,
          ChainClient.REQUEST_VALUE, ChainClient.CORRECT_SPEC_VALUE, ChainClient.INCORRECT_SPEC_VALUE);
    } catch (UserException e) {
      assertEquals(expectedMidServerException, e.getMessage());
    }
  }

  @Test(timeout = 1000)
  public void testChainMidReturnExceptionByCallbackWithCorrectSpec() {
    try {
      client.testReturnValue(ChainTest.SERVER_IDENTITY,
          ChainServiceHost.TEST_CHAIN_MID_RETURN_EXCEPTION_BY_CALLBACK_WITH_CORRECT_SPEC, ChainClient.REQUEST_VALUE,
          ChainClient.CORRECT_SPEC_VALUE, ChainClient.INCORRECT_SPEC_VALUE);
    } catch (UserException e) {
      assertEquals(expectedMidServerException, e.getMessage());
    }
  }
}
