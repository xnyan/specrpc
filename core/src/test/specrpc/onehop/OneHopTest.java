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

import static org.junit.Assert.assertEquals;

import java.util.concurrent.BrokenBarrierException;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import rpc.execption.UserException;
import specrpc.client.api.SpecRpcStatistics;

public class OneHopTest {
  private static final String SERVER_IDENTITY = "OneHopServer-ID";
  private static OneHopServer server = new OneHopServer(OneHopTest.SERVER_IDENTITY);
  private static OneHopClient client = new OneHopClient();
  private static boolean serverStarted = false;

  @Before
  public void beforeEach() {
    if (serverStarted == false) {
      server.start();
      serverStarted = true;
    }
    OneHopTest.client.getCallbackFactory().clearCallbackCount();
  }

  @After
  public void afterEach() {
    // server.terminate();
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
    // Thread.sleep(65 * 1000);

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

  // test actual return methods
  @Test(timeout = 1000)
  public void testActualReturnWithNoClientSpec() {
    // System.out.println("test Actual Return With No Client Spec");

    OneHopTest.client.getCallbackFactory().setBarrier(1);// make sure all callbacks finish

    String result = client.testReturnMethod(OneHopTest.SERVER_IDENTITY, OneHopServiceHost.TEST_ONLY_ACTUAL_RETURN,
        OneHopClient.TEST_REQUEST_VALUE);

    // assert result of callback
    assertEquals(result, OneHopClientCallback.CALLBACK_VALUE_PREFIX + OneHopServiceHost.RESPONSE_VALUE_PREFIX
        + OneHopClient.TEST_REQUEST_VALUE);
    // assert number of the callbacks that have been created
    assertEquals(1, OneHopTest.client.getCallbackFactory().getCallbackCount());
    // assert number of the rpcReturnValues (this should be the same as the number
    // of callbacks)
    assertEquals(1, OneHopTest.client.getCallbackFactory().getRPCReturnValues().size());
    // assert all the rpcReturnValues
    assertEquals(true, OneHopTest.client.getCallbackFactory().getRPCReturnValues()
        .contains(OneHopServiceHost.RESPONSE_VALUE_PREFIX + OneHopClient.TEST_REQUEST_VALUE));
  }

  @Test(timeout = 1000)
  public void testActualReturnWithCorrectClientSpec() {
    // System.out.println("test Actual Return With Correct Client Spec");

    OneHopTest.client.getCallbackFactory().setBarrier(1);// make sure all callbacks finish

    String result = client.testReturnMethod(OneHopTest.SERVER_IDENTITY, OneHopServiceHost.TEST_ONLY_ACTUAL_RETURN,
        OneHopClient.TEST_REQUEST_VALUE, OneHopClient.CORRECT_SPEC_VALUE);
    // assert result of callback
    assertEquals(result, OneHopClientCallback.CALLBACK_VALUE_PREFIX + OneHopServiceHost.RESPONSE_VALUE_PREFIX
        + OneHopClient.TEST_REQUEST_VALUE);
    // assert number of the callbacks that have been created
    assertEquals(1, OneHopTest.client.getCallbackFactory().getCallbackCount());
    // assert number of the rpcReturnValues (this should be the same as the number
    // of callbacks)
    assertEquals(1, OneHopTest.client.getCallbackFactory().getRPCReturnValues().size());
    // assert all the rpcReturnValues
    assertEquals(true, OneHopTest.client.getCallbackFactory().getRPCReturnValues()
        .contains(OneHopServiceHost.RESPONSE_VALUE_PREFIX + OneHopClient.TEST_REQUEST_VALUE));
  }

  @Test(timeout = 1000)
  public void testActualReturnWithIncorrectClientSpec() {
    // System.out.println("test Actual Return With Incorrect Client Spec");

    OneHopTest.client.getCallbackFactory().setBarrier(2);// make sure all callbacks finish

    String result = client.testReturnMethod(OneHopTest.SERVER_IDENTITY, OneHopServiceHost.TEST_ONLY_ACTUAL_RETURN,
        OneHopClient.TEST_REQUEST_VALUE, OneHopClient.INCORRECT_SPEC_VALUE_1);
    // assert result of callback
    assertEquals(result, OneHopClientCallback.CALLBACK_VALUE_PREFIX + OneHopServiceHost.RESPONSE_VALUE_PREFIX
        + OneHopClient.TEST_REQUEST_VALUE);
    // assert number of the callbacks that have been created
    assertEquals(2, OneHopTest.client.getCallbackFactory().getCallbackCount());
    // assert number of the rpcReturnValues (this should be the same as the number
    // of callbacks)
    assertEquals(2, OneHopTest.client.getCallbackFactory().getRPCReturnValues().size());
    // assert all the rpcReturnValues
    assertEquals(true,
        OneHopTest.client.getCallbackFactory().getRPCReturnValues().contains(OneHopClient.INCORRECT_SPEC_VALUE_1));
    assertEquals(true, OneHopTest.client.getCallbackFactory().getRPCReturnValues()
        .contains(OneHopServiceHost.RESPONSE_VALUE_PREFIX + OneHopClient.TEST_REQUEST_VALUE));
  }

  @Test(timeout = 1000)
  // Client speculate with both correct & incorrect value
  public void testActualReturnWithClientSpec() {
    // System.out.println("test Actual Return With Client Spec");

    OneHopTest.client.getCallbackFactory().setBarrier(2);// make sure all callbacks finish

    String result = client.testReturnMethod(OneHopTest.SERVER_IDENTITY, OneHopServiceHost.TEST_ONLY_ACTUAL_RETURN,
        OneHopClient.TEST_REQUEST_VALUE, OneHopClient.CORRECT_SPEC_VALUE, OneHopClient.INCORRECT_SPEC_VALUE_1);
    // assert result of callback
    assertEquals(result, OneHopClientCallback.CALLBACK_VALUE_PREFIX + OneHopServiceHost.RESPONSE_VALUE_PREFIX
        + OneHopClient.TEST_REQUEST_VALUE);
    // assert number of the callbacks that have been created
    assertEquals(2, OneHopTest.client.getCallbackFactory().getCallbackCount());
    // assert number of the rpcReturnValues (this should be the same as the number
    // of callbacks)
    assertEquals(2, OneHopTest.client.getCallbackFactory().getRPCReturnValues().size());
    // assert all the rpcReturnValues
    assertEquals(true,
        OneHopTest.client.getCallbackFactory().getRPCReturnValues().contains(OneHopClient.INCORRECT_SPEC_VALUE_1));
    assertEquals(true, OneHopTest.client.getCallbackFactory().getRPCReturnValues()
        .contains(OneHopServiceHost.RESPONSE_VALUE_PREFIX + OneHopClient.TEST_REQUEST_VALUE));
  }

  // test correct spec return method
  @Test(timeout = 1000)
  public void testCorrectSpecReturnWithNoClientSpec() {
    // System.out.println("test Correct Spec Return With No Client Spec");

    OneHopTest.client.getCallbackFactory().setBarrier(1);// make sure all callbacks finish

    String result = client.testReturnMethod(OneHopTest.SERVER_IDENTITY, OneHopServiceHost.TEST_CORRECT_SPEC_RETURN,
        OneHopClient.TEST_REQUEST_VALUE);

    // assert result of callback
    assertEquals(result, OneHopClientCallback.CALLBACK_VALUE_PREFIX + OneHopServiceHost.RESPONSE_VALUE_PREFIX
        + OneHopClient.TEST_REQUEST_VALUE);
    // assert number of the callbacks that have been created
    assertEquals(1, OneHopTest.client.getCallbackFactory().getCallbackCount());
    // assert number of the rpcReturnValues (this should be the same as the number
    // of callbacks)
    assertEquals(1, OneHopTest.client.getCallbackFactory().getRPCReturnValues().size());
    // assert all the rpcReturnValues
    assertEquals(true, OneHopTest.client.getCallbackFactory().getRPCReturnValues()
        .contains(OneHopServiceHost.RESPONSE_VALUE_PREFIX + OneHopClient.TEST_REQUEST_VALUE));
  }

  @Test(timeout = 1000)
  public void testCorrectSpecReturnWithCorrectClientSpec() {
    // System.out.println("test Correct Spec Return With Correct Client Spec");

    OneHopTest.client.getCallbackFactory().setBarrier(1);// make sure all callbacks finish

    String result = client.testReturnMethod(OneHopTest.SERVER_IDENTITY, OneHopServiceHost.TEST_CORRECT_SPEC_RETURN,
        OneHopClient.TEST_REQUEST_VALUE, OneHopClient.CORRECT_SPEC_VALUE);
    // assert result of callback
    assertEquals(result, OneHopClientCallback.CALLBACK_VALUE_PREFIX + OneHopServiceHost.RESPONSE_VALUE_PREFIX
        + OneHopClient.TEST_REQUEST_VALUE);
    // assert number of the callbacks that have been created
    assertEquals(1, OneHopTest.client.getCallbackFactory().getCallbackCount());
    // assert number of the rpcReturnValues (this should be the same as the number
    // of callbacks)
    assertEquals(1, OneHopTest.client.getCallbackFactory().getRPCReturnValues().size());
    // assert all the rpcReturnValues
    assertEquals(true, OneHopTest.client.getCallbackFactory().getRPCReturnValues()
        .contains(OneHopServiceHost.RESPONSE_VALUE_PREFIX + OneHopClient.TEST_REQUEST_VALUE));
  }

  @Test(timeout = 1000)
  public void testCorrectSpecReturnWithIncorrectClientSpec() {
    // System.out.println("test Correct Spec Return With Incorrect Client Spec");

    OneHopTest.client.getCallbackFactory().setBarrier(2);// make sure all callbacks finish

    String result = client.testReturnMethod(OneHopTest.SERVER_IDENTITY, OneHopServiceHost.TEST_CORRECT_SPEC_RETURN,
        OneHopClient.TEST_REQUEST_VALUE, OneHopClient.INCORRECT_SPEC_VALUE_1);
    // assert result of callback
    assertEquals(result, OneHopClientCallback.CALLBACK_VALUE_PREFIX + OneHopServiceHost.RESPONSE_VALUE_PREFIX
        + OneHopClient.TEST_REQUEST_VALUE);
    // assert number of the callbacks that have been created
    assertEquals(2, OneHopTest.client.getCallbackFactory().getCallbackCount());
    // assert number of the rpcReturnValues (this should be the same as the number
    // of callbacks)
    assertEquals(2, OneHopTest.client.getCallbackFactory().getRPCReturnValues().size());
    // assert all the rpcReturnValues
    assertEquals(true,
        OneHopTest.client.getCallbackFactory().getRPCReturnValues().contains(OneHopClient.INCORRECT_SPEC_VALUE_1));
    assertEquals(true, OneHopTest.client.getCallbackFactory().getRPCReturnValues()
        .contains(OneHopServiceHost.RESPONSE_VALUE_PREFIX + OneHopClient.TEST_REQUEST_VALUE));
  }

  @Test(timeout = 1000)
  // Client speculate with both correct & incorrect value
  public void testCorrecSpecReturnWithClientSpec() {
    // System.out.println("test Correct Spec Return With Client Spec");

    OneHopTest.client.getCallbackFactory().setBarrier(2);// make sure all callbacks finish

    String result = client.testReturnMethod(OneHopTest.SERVER_IDENTITY, OneHopServiceHost.TEST_CORRECT_SPEC_RETURN,
        OneHopClient.TEST_REQUEST_VALUE, OneHopClient.CORRECT_SPEC_VALUE, OneHopClient.INCORRECT_SPEC_VALUE_1);
    // assert result of callback
    assertEquals(result, OneHopClientCallback.CALLBACK_VALUE_PREFIX + OneHopServiceHost.RESPONSE_VALUE_PREFIX
        + OneHopClient.TEST_REQUEST_VALUE);
    // assert number of the callbacks that have been created
    assertEquals(2, OneHopTest.client.getCallbackFactory().getCallbackCount());
    // assert number of the rpcReturnValues (this should be the same as the number
    // of callbacks)
    assertEquals(2, OneHopTest.client.getCallbackFactory().getRPCReturnValues().size());
    // assert all the rpcReturnValues
    assertEquals(true,
        OneHopTest.client.getCallbackFactory().getRPCReturnValues().contains(OneHopClient.INCORRECT_SPEC_VALUE_1));
    assertEquals(true, OneHopTest.client.getCallbackFactory().getRPCReturnValues()
        .contains(OneHopServiceHost.RESPONSE_VALUE_PREFIX + OneHopClient.TEST_REQUEST_VALUE));
  }

  // test incorrect spec return method
  @Test(timeout = 1000)
  public void testInCorrectSpecReturnWithNoClientSpec() {
    // System.out.println("test InCorrect Spec Return With No Client Spec");

    OneHopTest.client.getCallbackFactory().setBarrier(2);// make sure all callbacks finish

    String result = client.testReturnMethod(OneHopTest.SERVER_IDENTITY, OneHopServiceHost.TEST_INCORRECT_SPEC_RETURN,
        OneHopClient.TEST_REQUEST_VALUE);

    // assert result of callback
    assertEquals(result, OneHopClientCallback.CALLBACK_VALUE_PREFIX + OneHopServiceHost.RESPONSE_VALUE_PREFIX
        + OneHopClient.TEST_REQUEST_VALUE);
    // assert number of the callbacks that have been created
    assertEquals(2, OneHopTest.client.getCallbackFactory().getCallbackCount());
    // assert number of the rpcReturnValues (this should be the same as the number
    // of callbacks)
    assertEquals(2, OneHopTest.client.getCallbackFactory().getRPCReturnValues().size());
    // assert all the rpcReturnValues
    assertEquals(true,
        OneHopTest.client.getCallbackFactory().getRPCReturnValues().contains(OneHopClient.INCORRECT_SPEC_VALUE_1));
    assertEquals(true, OneHopTest.client.getCallbackFactory().getRPCReturnValues()
        .contains(OneHopServiceHost.RESPONSE_VALUE_PREFIX + OneHopClient.TEST_REQUEST_VALUE));
  }

  @Test(timeout = 1000)
  public void testInCorrectSpecReturnWithCorrectClientSpec() {
    // System.out.println("test InCorrect Spec Return With Correct Client Spec");

    OneHopTest.client.getCallbackFactory().setBarrier(2);// make sure all callbacks finish

    String result = client.testReturnMethod(OneHopTest.SERVER_IDENTITY, OneHopServiceHost.TEST_INCORRECT_SPEC_RETURN,
        OneHopClient.TEST_REQUEST_VALUE, OneHopClient.CORRECT_SPEC_VALUE);
    // assert result of callback
    assertEquals(result, OneHopClientCallback.CALLBACK_VALUE_PREFIX + OneHopServiceHost.RESPONSE_VALUE_PREFIX
        + OneHopClient.TEST_REQUEST_VALUE);
    // assert number of the callbacks that have been created
    assertEquals(2, OneHopTest.client.getCallbackFactory().getCallbackCount());
    // assert number of the rpcReturnValues (this should be the same as the number
    // of callbacks)
    assertEquals(2, OneHopTest.client.getCallbackFactory().getRPCReturnValues().size());
    // assert all the rpcReturnValues
    assertEquals(true,
        OneHopTest.client.getCallbackFactory().getRPCReturnValues().contains(OneHopClient.INCORRECT_SPEC_VALUE_1));
    assertEquals(true, OneHopTest.client.getCallbackFactory().getRPCReturnValues()
        .contains(OneHopServiceHost.RESPONSE_VALUE_PREFIX + OneHopClient.TEST_REQUEST_VALUE));
  }

  @Test(timeout = 1000)
  public void testInCorrectSpecReturnWithIncorrectClientSpec() {
    // System.out.println("test InCorrect Spec Return With Incorrect Client Spec");

    OneHopTest.client.getCallbackFactory().setBarrier(2);// make sure all callbacks finish

    String result = client.testReturnMethod(OneHopTest.SERVER_IDENTITY, OneHopServiceHost.TEST_INCORRECT_SPEC_RETURN,
        OneHopClient.TEST_REQUEST_VALUE, OneHopClient.INCORRECT_SPEC_VALUE_1);
    // assert result of callback
    assertEquals(result, OneHopClientCallback.CALLBACK_VALUE_PREFIX + OneHopServiceHost.RESPONSE_VALUE_PREFIX
        + OneHopClient.TEST_REQUEST_VALUE);
    // assert number of the callbacks that have been created
    assertEquals(2, OneHopTest.client.getCallbackFactory().getCallbackCount());
    // assert number of the rpcReturnValues (this should be the same as the number
    // of callbacks)
    assertEquals(2, OneHopTest.client.getCallbackFactory().getRPCReturnValues().size());
    // assert all the rpcReturnValues
    assertEquals(true,
        OneHopTest.client.getCallbackFactory().getRPCReturnValues().contains(OneHopClient.INCORRECT_SPEC_VALUE_1));
    assertEquals(true, OneHopTest.client.getCallbackFactory().getRPCReturnValues()
        .contains(OneHopServiceHost.RESPONSE_VALUE_PREFIX + OneHopClient.TEST_REQUEST_VALUE));
  }

  @Test(timeout = 1000)
  // Client speculate with both correct & incorrect value
  public void testInCorrecSpecReturnWithClientSpec() {
    // System.out.println("test InCorrect Spec Return With Client Spec");

    OneHopTest.client.getCallbackFactory().setBarrier(2);// make sure all callbacks finish

    String result = client.testReturnMethod(OneHopTest.SERVER_IDENTITY, OneHopServiceHost.TEST_INCORRECT_SPEC_RETURN,
        OneHopClient.TEST_REQUEST_VALUE, OneHopClient.CORRECT_SPEC_VALUE, OneHopClient.INCORRECT_SPEC_VALUE_1);
    // assert result of callback
    assertEquals(result, OneHopClientCallback.CALLBACK_VALUE_PREFIX + OneHopServiceHost.RESPONSE_VALUE_PREFIX
        + OneHopClient.TEST_REQUEST_VALUE);
    // assert number of the callbacks that have been created
    assertEquals(2, OneHopTest.client.getCallbackFactory().getCallbackCount());
    // assert number of the rpcReturnValues (this should be the same as the number
    // of callbacks)
    assertEquals(2, OneHopTest.client.getCallbackFactory().getRPCReturnValues().size());
    // assert all the rpcReturnValues
    assertEquals(true,
        OneHopTest.client.getCallbackFactory().getRPCReturnValues().contains(OneHopClient.INCORRECT_SPEC_VALUE_1));
    assertEquals(true, OneHopTest.client.getCallbackFactory().getRPCReturnValues()
        .contains(OneHopServiceHost.RESPONSE_VALUE_PREFIX + OneHopClient.TEST_REQUEST_VALUE));
  }

  // test both correct & incorrect spec return methods
  @Test(timeout = 1000)
  public void testBothCorrectIncorrectSpecReturnWithNoClientSpec() {
    // System.out.println("test Both Correct Incorrect Spec Return With No Client
    // Spec");

    OneHopTest.client.getCallbackFactory().setBarrier(2);// make sure all callbacks finish

    String result = client.testReturnMethod(OneHopTest.SERVER_IDENTITY,
        OneHopServiceHost.TEST_BOTH_CORRECT_INCORRECT_SPEC_RETURN, OneHopClient.TEST_REQUEST_VALUE);
    // assert result of callback
    assertEquals(result, OneHopClientCallback.CALLBACK_VALUE_PREFIX + OneHopServiceHost.RESPONSE_VALUE_PREFIX
        + OneHopClient.TEST_REQUEST_VALUE);
    // assert number of the callbacks that have been created
    assertEquals(2, OneHopTest.client.getCallbackFactory().getCallbackCount());
    // assert number of the rpcReturnValues (this should be the same as the number
    // of callbacks)
    assertEquals(2, OneHopTest.client.getCallbackFactory().getRPCReturnValues().size());
    // assert all the rpcReturnValues
    assertEquals(true,
        OneHopTest.client.getCallbackFactory().getRPCReturnValues().contains(OneHopClient.INCORRECT_SPEC_VALUE_1));
    assertEquals(true, OneHopTest.client.getCallbackFactory().getRPCReturnValues()
        .contains(OneHopServiceHost.RESPONSE_VALUE_PREFIX + OneHopClient.TEST_REQUEST_VALUE));
  }

  @Test(timeout = 1000)
  public void testBothCorrectIncorrectSpecReturnWithCorrectClientSpec() {
    // System.out.println("test Both Correct Incorrect Spec Return With Correct
    // Client Spec");

    OneHopTest.client.getCallbackFactory().setBarrier(2);// make sure all callbacks finish

    String result = client.testReturnMethod(OneHopTest.SERVER_IDENTITY,
        OneHopServiceHost.TEST_BOTH_CORRECT_INCORRECT_SPEC_RETURN, OneHopClient.TEST_REQUEST_VALUE,
        OneHopClient.CORRECT_SPEC_VALUE);
    // assert result of callback
    assertEquals(result, OneHopClientCallback.CALLBACK_VALUE_PREFIX + OneHopServiceHost.RESPONSE_VALUE_PREFIX
        + OneHopClient.TEST_REQUEST_VALUE);
    // assert number of the callbacks that have been created
    assertEquals(2, OneHopTest.client.getCallbackFactory().getCallbackCount());
    // assert number of the rpcReturnValues (this should be the same as the number
    // of callbacks)
    assertEquals(2, OneHopTest.client.getCallbackFactory().getRPCReturnValues().size());
    // assert all the rpcReturnValues
    assertEquals(true,
        OneHopTest.client.getCallbackFactory().getRPCReturnValues().contains(OneHopClient.INCORRECT_SPEC_VALUE_1));
    assertEquals(true, OneHopTest.client.getCallbackFactory().getRPCReturnValues()
        .contains(OneHopServiceHost.RESPONSE_VALUE_PREFIX + OneHopClient.TEST_REQUEST_VALUE));
  }

  @Test(timeout = 1000)
  public void testBothCorrectIncorrectSpecReturnWithIncorrectClientSpec() {
    // System.out.println("test Both Correct Incorrect Spec Return With Incorrect
    // Client Spec");

    OneHopTest.client.getCallbackFactory().setBarrier(2);// make sure all callbacks finish

    String result = client.testReturnMethod(OneHopTest.SERVER_IDENTITY,
        OneHopServiceHost.TEST_BOTH_CORRECT_INCORRECT_SPEC_RETURN, OneHopClient.TEST_REQUEST_VALUE,
        OneHopClient.INCORRECT_SPEC_VALUE_1);
    // assert result of callback
    assertEquals(result, OneHopClientCallback.CALLBACK_VALUE_PREFIX + OneHopServiceHost.RESPONSE_VALUE_PREFIX
        + OneHopClient.TEST_REQUEST_VALUE);
    // assert number of the callbacks that have been created
    assertEquals(2, OneHopTest.client.getCallbackFactory().getCallbackCount());
    // assert number of the rpcReturnValues (this should be the same as the number
    // of callbacks)
    assertEquals(2, OneHopTest.client.getCallbackFactory().getRPCReturnValues().size());
    // assert all the rpcReturnValues
    assertEquals(true,
        OneHopTest.client.getCallbackFactory().getRPCReturnValues().contains(OneHopClient.INCORRECT_SPEC_VALUE_1));
    assertEquals(true, OneHopTest.client.getCallbackFactory().getRPCReturnValues()
        .contains(OneHopServiceHost.RESPONSE_VALUE_PREFIX + OneHopClient.TEST_REQUEST_VALUE));
  }

  @Test(timeout = 1000)
  // Client speculate with both correct & incorrect value
  public void testBothCorrectIncorrectSpecReturnWithClientSpec() {
    // System.out.println("test Both Correct Incorrect Spec Return With Client
    // Spec");

    OneHopTest.client.getCallbackFactory().setBarrier(2);// make sure all callbacks finish

    String result = client.testReturnMethod(OneHopTest.SERVER_IDENTITY,
        OneHopServiceHost.TEST_BOTH_CORRECT_INCORRECT_SPEC_RETURN, OneHopClient.TEST_REQUEST_VALUE,
        OneHopClient.CORRECT_SPEC_VALUE, OneHopClient.INCORRECT_SPEC_VALUE_1);
    // assert result of callback
    assertEquals(result, OneHopClientCallback.CALLBACK_VALUE_PREFIX + OneHopServiceHost.RESPONSE_VALUE_PREFIX
        + OneHopClient.TEST_REQUEST_VALUE);
    // assert number of the callbacks that have been created
    assertEquals(2, OneHopTest.client.getCallbackFactory().getCallbackCount());
    // assert number of the rpcReturnValues (this should be the same as the number
    // of callbacks)
    assertEquals(2, OneHopTest.client.getCallbackFactory().getRPCReturnValues().size());
    // assert all the rpcReturnValues
    assertEquals(true,
        OneHopTest.client.getCallbackFactory().getRPCReturnValues().contains(OneHopClient.INCORRECT_SPEC_VALUE_1));
    assertEquals(true, OneHopTest.client.getCallbackFactory().getRPCReturnValues()
        .contains(OneHopServiceHost.RESPONSE_VALUE_PREFIX + OneHopClient.TEST_REQUEST_VALUE));
  }

  // test multiple spec return methods
  @Test(timeout = 1000)
  public void testMultipleSpecReturnWithNoClientSpec() {
    // System.out.println("test Multiple Spec Return With No Client Spec");

    OneHopTest.client.getCallbackFactory().setBarrier(5);// make sure all callbacks finish

    String result = client.testReturnMethod(OneHopTest.SERVER_IDENTITY, OneHopServiceHost.TEST_MULTIPLE_SPEC_RETURN,
        OneHopClient.TEST_REQUEST_VALUE);
    // assert result of callback
    assertEquals(result, OneHopClientCallback.CALLBACK_VALUE_PREFIX + OneHopServiceHost.RESPONSE_VALUE_PREFIX
        + OneHopClient.TEST_REQUEST_VALUE);
    // assert number of the callbacks that have been created
    assertEquals(5, OneHopTest.client.getCallbackFactory().getCallbackCount());
    // assert number of the rpcReturnValues (this should be the same as the number
    // of callbacks)
    assertEquals(5, OneHopTest.client.getCallbackFactory().getRPCReturnValues().size());
    // assert all the rpcReturnValues
    assertEquals(true,
        OneHopTest.client.getCallbackFactory().getRPCReturnValues().contains(OneHopClient.INCORRECT_SPEC_VALUE_1));
    assertEquals(true,
        OneHopTest.client.getCallbackFactory().getRPCReturnValues().contains(OneHopClient.INCORRECT_SPEC_VALUE_2));
    assertEquals(true,
        OneHopTest.client.getCallbackFactory().getRPCReturnValues().contains(OneHopClient.INCORRECT_SPEC_VALUE_3));
    assertEquals(true,
        OneHopTest.client.getCallbackFactory().getRPCReturnValues().contains(OneHopClient.INCORRECT_SPEC_VALUE_4));
    assertEquals(true, OneHopTest.client.getCallbackFactory().getRPCReturnValues()
        .contains(OneHopServiceHost.RESPONSE_VALUE_PREFIX + OneHopClient.TEST_REQUEST_VALUE));
  }

  @Test(timeout = 1000)
  public void testMultipleSpecReturnWithCorrectClientSpec() {
    // System.out.println("test Multiple Spec Return With Correct Client Spec");

    OneHopTest.client.getCallbackFactory().setBarrier(5);// make sure all callbacks finish

    String result = client.testReturnMethod(OneHopTest.SERVER_IDENTITY, OneHopServiceHost.TEST_MULTIPLE_SPEC_RETURN,
        OneHopClient.TEST_REQUEST_VALUE, OneHopClient.CORRECT_SPEC_VALUE);
    // assert result of callback
    assertEquals(result, OneHopClientCallback.CALLBACK_VALUE_PREFIX + OneHopServiceHost.RESPONSE_VALUE_PREFIX
        + OneHopClient.TEST_REQUEST_VALUE);
    // assert number of the callbacks that have been created
    assertEquals(5, OneHopTest.client.getCallbackFactory().getCallbackCount());
    // assert number of the rpcReturnValues (this should be the same as the number
    // of callbacks)
    assertEquals(5, OneHopTest.client.getCallbackFactory().getRPCReturnValues().size());
    // assert all the rpcReturnValues
    assertEquals(true,
        OneHopTest.client.getCallbackFactory().getRPCReturnValues().contains(OneHopClient.INCORRECT_SPEC_VALUE_1));
    assertEquals(true,
        OneHopTest.client.getCallbackFactory().getRPCReturnValues().contains(OneHopClient.INCORRECT_SPEC_VALUE_2));
    assertEquals(true,
        OneHopTest.client.getCallbackFactory().getRPCReturnValues().contains(OneHopClient.INCORRECT_SPEC_VALUE_3));
    assertEquals(true,
        OneHopTest.client.getCallbackFactory().getRPCReturnValues().contains(OneHopClient.INCORRECT_SPEC_VALUE_4));
    assertEquals(true, OneHopTest.client.getCallbackFactory().getRPCReturnValues()
        .contains(OneHopServiceHost.RESPONSE_VALUE_PREFIX + OneHopClient.TEST_REQUEST_VALUE));
  }

  @Test(timeout = 1000)
  public void testMultipleSpecReturnWithIncorrectClientSpec() {
    // System.out.println("test Multiple Spec Return With Incorrect Client Spec");

    OneHopTest.client.getCallbackFactory().setBarrier(6);// make sure all callbacks finish

    String result = client.testReturnMethod(OneHopTest.SERVER_IDENTITY, OneHopServiceHost.TEST_MULTIPLE_SPEC_RETURN,
        OneHopClient.TEST_REQUEST_VALUE, OneHopClient.ONLY_CLIENT_INCORRECT_SPEC_VALUE,
        OneHopClient.INCORRECT_SPEC_VALUE_1);
    // assert result of callback
    assertEquals(result, OneHopClientCallback.CALLBACK_VALUE_PREFIX + OneHopServiceHost.RESPONSE_VALUE_PREFIX
        + OneHopClient.TEST_REQUEST_VALUE);
    // assert number of the callbacks that have been created
    assertEquals(6, OneHopTest.client.getCallbackFactory().getCallbackCount());
    // assert number of the rpcReturnValues (this should be the same as the number
    // of callbacks)
    assertEquals(6, OneHopTest.client.getCallbackFactory().getRPCReturnValues().size());
    // assert all the rpcReturnValues
    assertEquals(true, OneHopTest.client.getCallbackFactory().getRPCReturnValues()
        .contains(OneHopClient.ONLY_CLIENT_INCORRECT_SPEC_VALUE));
    assertEquals(true,
        OneHopTest.client.getCallbackFactory().getRPCReturnValues().contains(OneHopClient.INCORRECT_SPEC_VALUE_1));
    assertEquals(true,
        OneHopTest.client.getCallbackFactory().getRPCReturnValues().contains(OneHopClient.INCORRECT_SPEC_VALUE_2));
    assertEquals(true,
        OneHopTest.client.getCallbackFactory().getRPCReturnValues().contains(OneHopClient.INCORRECT_SPEC_VALUE_3));
    assertEquals(true,
        OneHopTest.client.getCallbackFactory().getRPCReturnValues().contains(OneHopClient.INCORRECT_SPEC_VALUE_4));
    assertEquals(true, OneHopTest.client.getCallbackFactory().getRPCReturnValues()
        .contains(OneHopServiceHost.RESPONSE_VALUE_PREFIX + OneHopClient.TEST_REQUEST_VALUE));
  }

  @Test(timeout = 1000)
  // Client speculate with both correct & incorrect value
  public void testMultipleSpecReturnWithClientSpec() {
    // System.out.println("test Multiple Spec Return With Client Spec");

    OneHopTest.client.getCallbackFactory().setBarrier(6);// make sure all callbacks finish

    String result = client.testReturnMethod(OneHopTest.SERVER_IDENTITY, OneHopServiceHost.TEST_MULTIPLE_SPEC_RETURN,
        OneHopClient.TEST_REQUEST_VALUE, OneHopClient.CORRECT_SPEC_VALUE, OneHopClient.ONLY_CLIENT_INCORRECT_SPEC_VALUE,
        OneHopClient.INCORRECT_SPEC_VALUE_1);
    // assert result of callback
    assertEquals(result, OneHopClientCallback.CALLBACK_VALUE_PREFIX + OneHopServiceHost.RESPONSE_VALUE_PREFIX
        + OneHopClient.TEST_REQUEST_VALUE);
    // assert number of the callbacks that have been created
    assertEquals(6, OneHopTest.client.getCallbackFactory().getCallbackCount());
    // assert number of the rpcReturnValues (this should be the same as the number
    // of callbacks)
    assertEquals(6, OneHopTest.client.getCallbackFactory().getRPCReturnValues().size());
    // assert all the rpcReturnValues
    assertEquals(true, OneHopTest.client.getCallbackFactory().getRPCReturnValues()
        .contains(OneHopClient.ONLY_CLIENT_INCORRECT_SPEC_VALUE));
    assertEquals(true,
        OneHopTest.client.getCallbackFactory().getRPCReturnValues().contains(OneHopClient.INCORRECT_SPEC_VALUE_1));
    assertEquals(true,
        OneHopTest.client.getCallbackFactory().getRPCReturnValues().contains(OneHopClient.INCORRECT_SPEC_VALUE_2));
    assertEquals(true,
        OneHopTest.client.getCallbackFactory().getRPCReturnValues().contains(OneHopClient.INCORRECT_SPEC_VALUE_3));
    assertEquals(true,
        OneHopTest.client.getCallbackFactory().getRPCReturnValues().contains(OneHopClient.INCORRECT_SPEC_VALUE_4));
    assertEquals(true, OneHopTest.client.getCallbackFactory().getRPCReturnValues()
        .contains(OneHopServiceHost.RESPONSE_VALUE_PREFIX + OneHopClient.TEST_REQUEST_VALUE));
  }

  // test specBlock()
  @Test(timeout = 1000)
  // Client speculate with both correct & incorrect value
  public void testSpecBlockInMultipleSpecReturnWithClientSpec() {
    // System.out.println("test SpecBlock in Multiple Spec Return With Client
    // Spec");
    OneHopClientSpecBlockCallbackFactory specBlockCallbackFactory = new OneHopClientSpecBlockCallbackFactory();
    specBlockCallbackFactory.setBarrier(6);// make sure all callbacks finish

    OneHopClientCallbackFactory factory = OneHopTest.client.getCallbackFactory();
    OneHopTest.client.setCallbackFactory(specBlockCallbackFactory);

    String result = client.testReturnMethod(OneHopTest.SERVER_IDENTITY, OneHopServiceHost.TEST_MULTIPLE_SPEC_RETURN,
        OneHopClient.TEST_REQUEST_VALUE, OneHopClient.CORRECT_SPEC_VALUE, OneHopClient.ONLY_CLIENT_INCORRECT_SPEC_VALUE,
        OneHopClient.INCORRECT_SPEC_VALUE_1);
    // assert result of callback
    assertEquals(result, OneHopClientCallback.CALLBACK_VALUE_PREFIX + OneHopServiceHost.RESPONSE_VALUE_PREFIX
        + OneHopClient.TEST_REQUEST_VALUE);
    // assert number of the callbacks that have been created
    assertEquals(6, OneHopTest.client.getCallbackFactory().getCallbackCount());
    // assert number of the rpcReturnValues (this should be the same as the number
    // of callbacks)
    assertEquals(6, OneHopTest.client.getCallbackFactory().getRPCReturnValues().size());
    // assert all the rpcReturnValues
    assertEquals(true, OneHopTest.client.getCallbackFactory().getRPCReturnValues()
        .contains(OneHopClient.ONLY_CLIENT_INCORRECT_SPEC_VALUE));
    assertEquals(true,
        OneHopTest.client.getCallbackFactory().getRPCReturnValues().contains(OneHopClient.INCORRECT_SPEC_VALUE_1));
    assertEquals(true,
        OneHopTest.client.getCallbackFactory().getRPCReturnValues().contains(OneHopClient.INCORRECT_SPEC_VALUE_2));
    assertEquals(true,
        OneHopTest.client.getCallbackFactory().getRPCReturnValues().contains(OneHopClient.INCORRECT_SPEC_VALUE_3));
    assertEquals(true,
        OneHopTest.client.getCallbackFactory().getRPCReturnValues().contains(OneHopClient.INCORRECT_SPEC_VALUE_4));
    assertEquals(true, OneHopTest.client.getCallbackFactory().getRPCReturnValues()
        .contains(OneHopServiceHost.RESPONSE_VALUE_PREFIX + OneHopClient.TEST_REQUEST_VALUE));
    assertEquals(1, specBlockCallbackFactory.getPassSpecBlockNum());
    assertEquals(5, specBlockCallbackFactory.getFailSpecBlockNum());

    OneHopTest.client.setCallbackFactory(factory);
  }

  // test exception return
  @Test(timeout = 1000)
  public void testExceptionReturnWithNoClientSpec() {
    // System.out.println("test Exception Return With No Client Spec");

    OneHopTest.client.getCallbackFactory().setBarrier(1);// make sure all callbacks finish

    String result = null;
    try {
      result = client.testEception(OneHopTest.SERVER_IDENTITY, OneHopServiceHost.TEST_EXCEPTION_RETURN,
          OneHopClient.TEST_REQUEST_VALUE);
    } catch (UserException e) {
      // assert exception
      assertEquals(e.toString(),
          UserException.class.getName() + ": " + OneHopServiceHost.EXCEPTION_PREFIX + OneHopClient.TEST_REQUEST_VALUE);
      // assert number of the callbacks that have been created
      assertEquals(0, OneHopTest.client.getCallbackFactory().getCallbackCount());
      // assert number of the rpcReturnValues (this should be the same as the number
      // of callbacks)
      assertEquals(0, OneHopTest.client.getCallbackFactory().getRPCReturnValues().size());
      return;
    }
    assertEquals(true, false);
    System.out.println("Test Failure: " + result);
  }

  @Test(timeout = 1000)
  public void testExceptionReturnWithClientSpec() {
    // System.out.println("test Exception Return With Client Spec");

    OneHopTest.client.getCallbackFactory().setBarrier(2);// make sure all callbacks finish

    String result = null;
    try {
      result = client.testEception(OneHopTest.SERVER_IDENTITY, OneHopServiceHost.TEST_EXCEPTION_RETURN,
          OneHopClient.TEST_REQUEST_VALUE, OneHopClient.CORRECT_SPEC_VALUE, OneHopClient.INCORRECT_SPEC_VALUE_1);
    } catch (UserException e) {
      // assert exception
      assertEquals(e.toString(),
          UserException.class.getName() + ": " + OneHopServiceHost.EXCEPTION_PREFIX + OneHopClient.TEST_REQUEST_VALUE);
      // assert number of the callbacks that have been created
      assertEquals(2, OneHopTest.client.getCallbackFactory().getCallbackCount());
      // assert number of the rpcReturnValues (this should be the same as the number
      // of callbacks)
      assertEquals(2, OneHopTest.client.getCallbackFactory().getRPCReturnValues().size());
      // assert all the rpcReturnValues
      assertEquals(true,
          OneHopTest.client.getCallbackFactory().getRPCReturnValues().contains(OneHopClient.CORRECT_SPEC_VALUE));
      assertEquals(true,
          OneHopTest.client.getCallbackFactory().getRPCReturnValues().contains(OneHopClient.INCORRECT_SPEC_VALUE_1));
      return;
    }
    assertEquals(true, false);
    System.out.println("Test Failure: " + result);
  }

  // test exception return after server spec return
  @Test(timeout = 1000)
  public void testExceptionAfterSpecReturnWithNoClientSpec() {
    // System.out.println("test Exception After Spec Return With No Client Spec");

    OneHopTest.client.getCallbackFactory().setBarrier(5);// make sure all callbacks finish

    String result = null;
    try {
      result = client.testEception(OneHopTest.SERVER_IDENTITY, OneHopServiceHost.TEST_EXCEPTION_AFTER_SPEC_RETURN,
          OneHopClient.TEST_REQUEST_VALUE);
    } catch (UserException e) {
      // assert exception
      assertEquals(e.toString(),
          UserException.class.getName() + ": " + OneHopServiceHost.EXCEPTION_PREFIX + OneHopClient.TEST_REQUEST_VALUE);

      try {
        OneHopTest.client.getCallbackFactory().getBarrier().await();// allow all callbacks finish
      } catch (InterruptedException e1) {
        e1.printStackTrace();
      } catch (BrokenBarrierException e1) {
        e1.printStackTrace();
      }

      // assert number of the callbacks that have been created
      assertEquals(4, OneHopTest.client.getCallbackFactory().getCallbackCount());
      // assert number of the rpcReturnValues (this should be the same as the number
      // of callbacks)
      assertEquals(4, OneHopTest.client.getCallbackFactory().getRPCReturnValues().size());
      // assert all the rpcReturnValues
      assertEquals(true,
          OneHopTest.client.getCallbackFactory().getRPCReturnValues().contains(OneHopClient.INCORRECT_SPEC_VALUE_1));
      assertEquals(true,
          OneHopTest.client.getCallbackFactory().getRPCReturnValues().contains(OneHopClient.INCORRECT_SPEC_VALUE_2));
      assertEquals(true,
          OneHopTest.client.getCallbackFactory().getRPCReturnValues().contains(OneHopClient.INCORRECT_SPEC_VALUE_3));
      assertEquals(true,
          OneHopTest.client.getCallbackFactory().getRPCReturnValues().contains(OneHopClient.INCORRECT_SPEC_VALUE_4));
      return;
    }
    assertEquals(true, false);
    System.out.println("Test Failure: " + result);
  }

  @Test(timeout = 1000)
  public void testExceptionAfterSpecReturnWithClientSpec() {
    // System.out.println("test Exception After Spec Return With Client Spec");

    OneHopTest.client.getCallbackFactory().setBarrier(6);// make sure all callbacks finish

    String result = null;
    try {
      result = client.testEception(OneHopTest.SERVER_IDENTITY, OneHopServiceHost.TEST_EXCEPTION_AFTER_SPEC_RETURN,
          OneHopClient.TEST_REQUEST_VALUE, OneHopClient.CORRECT_SPEC_VALUE, OneHopClient.INCORRECT_SPEC_VALUE_1);
    } catch (UserException e) {
      // assert exception
      assertEquals(e.toString(),
          UserException.class.getName() + ": " + OneHopServiceHost.EXCEPTION_PREFIX + OneHopClient.TEST_REQUEST_VALUE);

      try {
        OneHopTest.client.getCallbackFactory().getBarrier().await();// allow all callbacks finish
      } catch (InterruptedException e1) {
        e1.printStackTrace();
      } catch (BrokenBarrierException e1) {
        e1.printStackTrace();
      }

      // assert number of the callbacks that have been created
      assertEquals(5, OneHopTest.client.getCallbackFactory().getCallbackCount());
      // assert number of the rpcReturnValues (this should be the same as the number
      // of callbacks)
      assertEquals(5, OneHopTest.client.getCallbackFactory().getRPCReturnValues().size());
      // assert all the rpcReturnValues
      assertEquals(true,
          OneHopTest.client.getCallbackFactory().getRPCReturnValues().contains(OneHopClient.INCORRECT_SPEC_VALUE_1));
      assertEquals(true,
          OneHopTest.client.getCallbackFactory().getRPCReturnValues().contains(OneHopClient.INCORRECT_SPEC_VALUE_2));
      assertEquals(true,
          OneHopTest.client.getCallbackFactory().getRPCReturnValues().contains(OneHopClient.INCORRECT_SPEC_VALUE_3));
      assertEquals(true,
          OneHopTest.client.getCallbackFactory().getRPCReturnValues().contains(OneHopClient.INCORRECT_SPEC_VALUE_4));
      assertEquals(true,
          OneHopTest.client.getCallbackFactory().getRPCReturnValues().contains(OneHopClient.CORRECT_SPEC_VALUE));
      return;
    }
    assertEquals(true, false);
    System.out.println("Test Failure: " + result);
  }

}
