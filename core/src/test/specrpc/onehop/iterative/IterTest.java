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

import java.util.ArrayList;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicInteger;

import junit.framework.Assert;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

import rpc.execption.UserException;
import specrpc.client.api.SpecRpcStatistics;
import specrpc.common.RpcSignature;

public class IterTest {

  public static final String SERVER_IDENTITY = "IterTestServer-ID";
  private static IterServer server = new IterServer(IterTest.SERVER_IDENTITY);
  private static IterClient client = new IterClient();
  private static boolean serverStarted = false;
  private final int FAIL_NUM_INVALID = -1;// in some cases the actual num of fail callback is not determined

  @Before
  public void beforeEach() {
    if (serverStarted == false) {
      server.start();
      serverStarted = true;
    }
  }

  @After
  public void afterEach() {
    try {
      // allow incorrectly speculative callback to finish before Server/Client
      // terminate. Otherwise exception may happen in multi spec return from server
      // because the shutdown of client thread pool terminates kills the incorrectly
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

  private String createExpectedResult(int iterDepth) {
    String result = "";
    for (int i = iterDepth; i > 0; i--) {
      result += IterClient.CALLBACK_RESULT_PREFIX + i + " " + IterClient.CORRECT_SPEC_VALUE;
    }
    return result;
  }

  // Note: as the depth increases, the test case will cost more time because
  // incorrect speculative callbacks consume more computing resource
  @Test(timeout = 20000)
  public void test_2_DepthIterReturn() {
    final int depth = 2;
    invokeBatchReturnTest(depth, IterServer.getTestReturnValue());
    invokeBatchReturnTest(depth, IterServer.getTestIncorrectSpecReturnValue());
    invokeBatchReturnTest(depth, IterServer.getTestCorrectSpecReturnValue());
    invokeBatchReturnTest(depth, IterServer.getTestMultiIncorrectSpecReturnValue());
    invokeBatchReturnTest(depth, IterServer.getTestMultiSpecReturnValueWithCorrectSpec());
  }

  @Test(timeout = 20000)
  public void test_3_DepthIterReturn() {
    final int depth = 3;
    invokeBatchReturnTest(depth, IterServer.getTestReturnValue());
    invokeBatchReturnTest(depth, IterServer.getTestIncorrectSpecReturnValue());
    invokeBatchReturnTest(depth, IterServer.getTestCorrectSpecReturnValue());
    invokeBatchReturnTest(depth, IterServer.getTestMultiIncorrectSpecReturnValue());
    invokeBatchReturnTest(depth, IterServer.getTestMultiSpecReturnValueWithCorrectSpec());
  }

  @Test(timeout = 20000)
  public void test_4_DepthIterReturn() {
    final int depth = 4;
    invokeBatchReturnTest(depth, IterServer.getTestReturnValue());
    invokeBatchReturnTest(depth, IterServer.getTestIncorrectSpecReturnValue());
    invokeBatchReturnTest(depth, IterServer.getTestCorrectSpecReturnValue());
    invokeBatchReturnTest(depth, IterServer.getTestMultiIncorrectSpecReturnValue());
    invokeBatchReturnTest(depth, IterServer.getTestMultiSpecReturnValueWithCorrectSpec());
  }

  @Test(timeout = 20000)
  public void test_5_DepthIterReturn() {
    final int depth = 5;
    invokeBatchReturnTest(depth, IterServer.getTestReturnValue());
    invokeBatchReturnTest(depth, IterServer.getTestIncorrectSpecReturnValue());
    invokeBatchReturnTest(depth, IterServer.getTestCorrectSpecReturnValue());
    invokeBatchReturnTest(depth, IterServer.getTestMultiIncorrectSpecReturnValue());
    invokeBatchReturnTest(depth, IterServer.getTestMultiSpecReturnValueWithCorrectSpec());
  }

  // test specBlock() in only in server side
  @Test(timeout = 20000)
  public void test_2_DepthIterServerSpecBlockReturn() {
    final int depth = 2;
    invokeBatchReturnTest(depth, IterServer.getTestSpecBlockReturnValue());
    invokeBatchReturnTest(depth, IterServer.getTestSpecBlockIncorrectSpecReturnValue());
    invokeBatchReturnTest(depth, IterServer.getTestSpecBlockCorrectSpecReturnValue());
    invokeBatchReturnTest(depth, IterServer.getTestSpecBlockMultiIncorrectSpecReturnValue());
    invokeBatchReturnTest(depth, IterServer.getTestSpecBlockMultiSpecReturnValueWithCorrectSpec());
  }

  @Test(timeout = 20000)
  public void test_3_DepthIterServerSpecBlockReturn() {
    final int depth = 3;
    invokeBatchReturnTest(depth, IterServer.getTestSpecBlockReturnValue());
    invokeBatchReturnTest(depth, IterServer.getTestSpecBlockIncorrectSpecReturnValue());
    invokeBatchReturnTest(depth, IterServer.getTestSpecBlockCorrectSpecReturnValue());
    invokeBatchReturnTest(depth, IterServer.getTestSpecBlockMultiIncorrectSpecReturnValue());
    invokeBatchReturnTest(depth, IterServer.getTestSpecBlockMultiSpecReturnValueWithCorrectSpec());
  }

  @Test(timeout = 20000)
  public void test_4_DepthIterServerSpecBlockReturn() {
    final int depth = 4;
    invokeBatchReturnTest(depth, IterServer.getTestSpecBlockReturnValue());
    invokeBatchReturnTest(depth, IterServer.getTestSpecBlockIncorrectSpecReturnValue());
    invokeBatchReturnTest(depth, IterServer.getTestSpecBlockCorrectSpecReturnValue());
    invokeBatchReturnTest(depth, IterServer.getTestSpecBlockMultiIncorrectSpecReturnValue());
    invokeBatchReturnTest(depth, IterServer.getTestSpecBlockMultiSpecReturnValueWithCorrectSpec());
  }

  @Test(timeout = 20000)
  public void test_5_DepthIterServerSpecBlockReturn() {
    final int depth = 5;
    invokeBatchReturnTest(depth, IterServer.getTestSpecBlockReturnValue());
    invokeBatchReturnTest(depth, IterServer.getTestSpecBlockIncorrectSpecReturnValue());
    invokeBatchReturnTest(depth, IterServer.getTestSpecBlockCorrectSpecReturnValue());
    invokeBatchReturnTest(depth, IterServer.getTestSpecBlockMultiIncorrectSpecReturnValue());
    invokeBatchReturnTest(depth, IterServer.getTestSpecBlockMultiSpecReturnValueWithCorrectSpec());
  }

  private void invokeBatchReturnTest(final int depth, final RpcSignature methodSignature) {
    invokeReturnTest(depth, methodSignature, IterClient.TEST_REQUEST_VALUE);
    invokeReturnTest(depth, methodSignature, IterClient.TEST_REQUEST_VALUE, IterClient.INCORRECT_SPEC_VALUE);
    invokeReturnTest(depth, methodSignature, IterClient.TEST_REQUEST_VALUE, IterClient.CORRECT_SPEC_VALUE);
    invokeReturnTest(depth, methodSignature, IterClient.TEST_REQUEST_VALUE, IterClient.CORRECT_SPEC_VALUE,
        IterClient.INCORRECT_SPEC_VALUE);
  }

  private void invokeReturnTest(final int depth, final RpcSignature methodSignature, final String requestValue,
      Object... predictedValues) {
    try {
      final String expected = createExpectedResult(depth);
      ArrayList<Object> speculativeValues = new ArrayList<Object>();
      for (Object value : predictedValues) {
        speculativeValues.add(value);
      }
      String actual = client.testIterCallPattern(
          new IterClientCallbackFactory(depth, methodSignature, requestValue, speculativeValues),
          IterTest.SERVER_IDENTITY, methodSignature, requestValue, speculativeValues);
      Assert.assertEquals(expected, actual);
    } catch (UserException e) {
      e.printStackTrace();
    }
  }

  // test specBlock() in callback
  @Test(timeout = 20000)
  public void test_2_DepthIterClientCallbackSpecBlock() {
    final int depth = 2;
    invokeBatchCallbackSpecBlockTest(depth);
  }

  @Test(timeout = 20000)
  public void test_3_DepthIterClientCallbackSpecBlock() {
    final int depth = 3;
    invokeBatchCallbackSpecBlockTest(depth);
  }

  @Test(timeout = 20000)
  public void test_4_DepthIterClientCallbackSpecBlock() {
    final int depth = 4;
    invokeBatchCallbackSpecBlockTest(depth);
  }

  @Test(timeout = 20000)
  public void test_5_DepthIterClientCallbackSpecBlock() {
    final int depth = 5;
    invokeBatchCallbackSpecBlockTest(depth);
  }

  private void invokeBatchCallbackSpecBlockTest(final int depth) {
    RpcSignature methodSignature = IterServer.getTestReturnValue();
    invokeCallbackSpecBlockTest(depth, 1, depth, 0, methodSignature, IterClient.TEST_REQUEST_VALUE);
    invokeCallbackSpecBlockTest(depth, 2, depth, depth, methodSignature, IterClient.TEST_REQUEST_VALUE,
        IterClient.INCORRECT_SPEC_VALUE);
    invokeCallbackSpecBlockTest(depth, 1, depth, 0, methodSignature, IterClient.TEST_REQUEST_VALUE,
        IterClient.CORRECT_SPEC_VALUE);
    invokeCallbackSpecBlockTest(depth, 2, depth, depth, methodSignature, IterClient.TEST_REQUEST_VALUE,
        IterClient.CORRECT_SPEC_VALUE, IterClient.INCORRECT_SPEC_VALUE);

    methodSignature = IterServer.getTestCorrectSpecReturnValue();
    invokeCallbackSpecBlockTest(depth, 1, depth, 0, methodSignature, IterClient.TEST_REQUEST_VALUE);
    invokeCallbackSpecBlockTest(depth, 2, depth, depth, methodSignature, IterClient.TEST_REQUEST_VALUE,
        IterClient.INCORRECT_SPEC_VALUE);
    invokeCallbackSpecBlockTest(depth, 1, depth, 0, methodSignature, IterClient.TEST_REQUEST_VALUE,
        IterClient.CORRECT_SPEC_VALUE);
    invokeCallbackSpecBlockTest(depth, 2, depth, depth, methodSignature, IterClient.TEST_REQUEST_VALUE,
        IterClient.CORRECT_SPEC_VALUE, IterClient.INCORRECT_SPEC_VALUE);

    methodSignature = IterServer.getTestIncorrectSpecReturnValue();
    invokeCallbackSpecBlockTest(depth, 2, depth, depth, methodSignature, IterClient.TEST_REQUEST_VALUE);
    invokeCallbackSpecBlockTest(depth, 3, depth, depth * 2, methodSignature, IterClient.TEST_REQUEST_VALUE,
        IterClient.INCORRECT_SPEC_VALUE);
    invokeCallbackSpecBlockTest(depth, 2, depth, depth, methodSignature, IterClient.TEST_REQUEST_VALUE,
        IterClient.CORRECT_SPEC_VALUE);
    invokeCallbackSpecBlockTest(depth, 3, depth, depth * 2, methodSignature, IterClient.TEST_REQUEST_VALUE,
        IterClient.CORRECT_SPEC_VALUE, IterClient.INCORRECT_SPEC_VALUE);

    methodSignature = IterServer.getTestMultiIncorrectSpecReturnValue();
    invokeCallbackSpecBlockTest(depth, 5, depth, depth * 4, methodSignature, IterClient.TEST_REQUEST_VALUE);
    invokeCallbackSpecBlockTest(depth, 6, depth, depth * 5, methodSignature, IterClient.TEST_REQUEST_VALUE,
        IterClient.INCORRECT_SPEC_VALUE);
    invokeCallbackSpecBlockTest(depth, 5, depth, depth * 4, methodSignature, IterClient.TEST_REQUEST_VALUE,
        IterClient.CORRECT_SPEC_VALUE);
    invokeCallbackSpecBlockTest(depth, 6, depth, depth * 5, methodSignature, IterClient.TEST_REQUEST_VALUE,
        IterClient.CORRECT_SPEC_VALUE, IterClient.INCORRECT_SPEC_VALUE);

    methodSignature = IterServer.getTestMultiSpecReturnValueWithCorrectSpec();
    invokeCallbackSpecBlockTest(depth, 5, depth, depth * 4, methodSignature, IterClient.TEST_REQUEST_VALUE);
    invokeCallbackSpecBlockTest(depth, 6, depth, depth * 5, methodSignature, IterClient.TEST_REQUEST_VALUE,
        IterClient.INCORRECT_SPEC_VALUE);
    invokeCallbackSpecBlockTest(depth, 5, depth, depth * 4, methodSignature, IterClient.TEST_REQUEST_VALUE,
        IterClient.CORRECT_SPEC_VALUE);
    invokeCallbackSpecBlockTest(depth, 6, depth, depth * 5, methodSignature, IterClient.TEST_REQUEST_VALUE,
        IterClient.CORRECT_SPEC_VALUE, IterClient.INCORRECT_SPEC_VALUE);
  }

  private void invokeCallbackSpecBlockTest(final int depth, final int callbackParties, final int expectedPassNum,
      final int expectedFailNum, final RpcSignature methodSignature, final String requestValue,
      Object... predictedValues) {
    try {
      final String expected = createExpectedResult(depth);
      ArrayList<Object> speculativeValues = new ArrayList<Object>();
      for (Object value : predictedValues) {
        speculativeValues.add(value);
      }
      CyclicBarrier barrier = new CyclicBarrier(callbackParties);
      AtomicInteger passSpecBlockNum = new AtomicInteger(0);
      AtomicInteger failSpecBlockNum = new AtomicInteger(0);
      String actual = client.testIterCallPattern(
          new IterClientSpecBlockCallbackFactory(depth, methodSignature, requestValue, speculativeValues, barrier,
              passSpecBlockNum, failSpecBlockNum),
          IterTest.SERVER_IDENTITY, methodSignature, requestValue, speculativeValues);

      // barrier.await();

      Assert.assertEquals(expected, actual);
      Assert.assertEquals(expectedPassNum, passSpecBlockNum.get());
      if (expectedFailNum != FAIL_NUM_INVALID) {
        Assert.assertEquals(expectedFailNum, failSpecBlockNum.get());
      }

    } catch (UserException e) {
      e.printStackTrace();
    }
  }

  // test specBlock in Callback when there is no specBlock in the first Callback
  @Test(timeout = 20000)
  public void test_2_DepthIterClientNotFirstCallbackSpecBlock() {
    final int depth = 2;
    invokeBatchNotFirstCallbackSpecBlockTest(depth);
  }

  @Test(timeout = 20000)
  public void test_3_DepthIterClientNotFirstCallbackSpecBlock() {
    final int depth = 3;
    invokeBatchNotFirstCallbackSpecBlockTest(depth);
  }

  @Test(timeout = 20000)
  public void test_4_DepthIterClientNotFirstCallbackSpecBlock() {
    final int depth = 4;
    invokeBatchNotFirstCallbackSpecBlockTest(depth);
  }

  @Test(timeout = 20000)
  public void test_5_DepthIterClientNotFirstCallbackSpecBlock() {
    final int depth = 5;
    invokeBatchNotFirstCallbackSpecBlockTest(depth);
  }

  private void invokeBatchNotFirstCallbackSpecBlockTest(final int depth) {
    RpcSignature methodSignature = IterServer.getTestReturnValue();
    invokeNotFirstCallbackSpecBlockTest(depth, 1, depth - 1, 0, methodSignature, IterClient.TEST_REQUEST_VALUE);
    invokeNotFirstCallbackSpecBlockTest(depth, 2, depth - 1, this.FAIL_NUM_INVALID, methodSignature,
        IterClient.TEST_REQUEST_VALUE, IterClient.INCORRECT_SPEC_VALUE);
    invokeNotFirstCallbackSpecBlockTest(depth, 1, depth - 1, 0, methodSignature, IterClient.TEST_REQUEST_VALUE,
        IterClient.CORRECT_SPEC_VALUE);
    invokeNotFirstCallbackSpecBlockTest(depth, 2, depth - 1, this.FAIL_NUM_INVALID, methodSignature,
        IterClient.TEST_REQUEST_VALUE, IterClient.CORRECT_SPEC_VALUE, IterClient.INCORRECT_SPEC_VALUE);

    methodSignature = IterServer.getTestCorrectSpecReturnValue();
    invokeNotFirstCallbackSpecBlockTest(depth, 1, depth - 1, 0, methodSignature, IterClient.TEST_REQUEST_VALUE);
    invokeNotFirstCallbackSpecBlockTest(depth, 2, depth - 1, this.FAIL_NUM_INVALID, methodSignature,
        IterClient.TEST_REQUEST_VALUE, IterClient.INCORRECT_SPEC_VALUE);
    invokeNotFirstCallbackSpecBlockTest(depth, 1, depth - 1, 0, methodSignature, IterClient.TEST_REQUEST_VALUE,
        IterClient.CORRECT_SPEC_VALUE);
    invokeNotFirstCallbackSpecBlockTest(depth, 2, depth - 1, this.FAIL_NUM_INVALID, methodSignature,
        IterClient.TEST_REQUEST_VALUE, IterClient.CORRECT_SPEC_VALUE, IterClient.INCORRECT_SPEC_VALUE);

    methodSignature = IterServer.getTestIncorrectSpecReturnValue();
    invokeNotFirstCallbackSpecBlockTest(depth, 2, depth - 1, this.FAIL_NUM_INVALID, methodSignature,
        IterClient.TEST_REQUEST_VALUE);
    invokeNotFirstCallbackSpecBlockTest(depth, 3, depth - 1, this.FAIL_NUM_INVALID, methodSignature,
        IterClient.TEST_REQUEST_VALUE, IterClient.INCORRECT_SPEC_VALUE);
    invokeNotFirstCallbackSpecBlockTest(depth, 2, depth - 1, this.FAIL_NUM_INVALID, methodSignature,
        IterClient.TEST_REQUEST_VALUE, IterClient.CORRECT_SPEC_VALUE);
    invokeNotFirstCallbackSpecBlockTest(depth, 3, depth - 1, this.FAIL_NUM_INVALID, methodSignature,
        IterClient.TEST_REQUEST_VALUE, IterClient.CORRECT_SPEC_VALUE, IterClient.INCORRECT_SPEC_VALUE);

    methodSignature = IterServer.getTestMultiIncorrectSpecReturnValue();
    invokeNotFirstCallbackSpecBlockTest(depth, 5, depth - 1, this.FAIL_NUM_INVALID, methodSignature,
        IterClient.TEST_REQUEST_VALUE);
    invokeNotFirstCallbackSpecBlockTest(depth, 6, depth - 1, this.FAIL_NUM_INVALID, methodSignature,
        IterClient.TEST_REQUEST_VALUE, IterClient.INCORRECT_SPEC_VALUE);
    invokeNotFirstCallbackSpecBlockTest(depth, 5, depth - 1, this.FAIL_NUM_INVALID, methodSignature,
        IterClient.TEST_REQUEST_VALUE, IterClient.CORRECT_SPEC_VALUE);
    invokeNotFirstCallbackSpecBlockTest(depth, 6, depth - 1, this.FAIL_NUM_INVALID, methodSignature,
        IterClient.TEST_REQUEST_VALUE, IterClient.CORRECT_SPEC_VALUE, IterClient.INCORRECT_SPEC_VALUE);

    methodSignature = IterServer.getTestMultiSpecReturnValueWithCorrectSpec();
    invokeNotFirstCallbackSpecBlockTest(depth, 5, depth - 1, this.FAIL_NUM_INVALID, methodSignature,
        IterClient.TEST_REQUEST_VALUE);
    invokeNotFirstCallbackSpecBlockTest(depth, 6, depth - 1, this.FAIL_NUM_INVALID, methodSignature,
        IterClient.TEST_REQUEST_VALUE, IterClient.INCORRECT_SPEC_VALUE);
    invokeNotFirstCallbackSpecBlockTest(depth, 5, depth - 1, this.FAIL_NUM_INVALID, methodSignature,
        IterClient.TEST_REQUEST_VALUE, IterClient.CORRECT_SPEC_VALUE);
    invokeNotFirstCallbackSpecBlockTest(depth, 6, depth - 1, this.FAIL_NUM_INVALID, methodSignature,
        IterClient.TEST_REQUEST_VALUE, IterClient.CORRECT_SPEC_VALUE, IterClient.INCORRECT_SPEC_VALUE);
  }

  private void invokeNotFirstCallbackSpecBlockTest(final int depth, final int callbackParties,
      final int expectedPassNum, final int expectedFailNum, final RpcSignature methodSignature,
      final String requestValue, Object... predictedValues) {
    try {
      final String expected = createExpectedResult(depth);
      ArrayList<Object> speculativeValues = new ArrayList<Object>();
      for (Object value : predictedValues) {
        speculativeValues.add(value);
      }
      CyclicBarrier barrier = new CyclicBarrier(callbackParties);
      AtomicInteger passSpecBlockNum = new AtomicInteger(0);
      AtomicInteger failSpecBlockNum = new AtomicInteger(0);
      String actual = client.testIterCallPattern(
          new IterClientPreSpecBlockCallbackFactory(depth, methodSignature, requestValue, speculativeValues, barrier,
              passSpecBlockNum, failSpecBlockNum),
          IterTest.SERVER_IDENTITY, methodSignature, requestValue, speculativeValues);

      Assert.assertEquals(expected, actual);
      Assert.assertEquals(expectedPassNum, passSpecBlockNum.get());
      if (expectedFailNum != FAIL_NUM_INVALID) {
        Assert.assertEquals(expectedFailNum, failSpecBlockNum.get());
      }

    } catch (UserException e) {
      e.printStackTrace();
    }
  }

  // test exception return
  @Test(timeout = 20000)
  public void test_2_DepthIterExceptionReturn() {
    final int depth = 2;
    invokeBatchExceptionTest(depth, IterServer.getTestReturnException());
    invokeBatchExceptionTest(depth, IterServer.getTestReturnExceptionAfterSpecReturn());
  }

  @Test(timeout = 20000)
  public void test_3_DepthIterExceptionReturn() {
    final int depth = 3;
    invokeBatchExceptionTest(depth, IterServer.getTestReturnException());
    invokeBatchExceptionTest(depth, IterServer.getTestReturnExceptionAfterSpecReturn());
  }

  @Test(timeout = 20000)
  public void test_4_DepthIterExceptionReturn() {
    final int depth = 4;
    invokeBatchExceptionTest(depth, IterServer.getTestReturnException());
    invokeBatchExceptionTest(depth, IterServer.getTestReturnExceptionAfterSpecReturn());
  }

  @Test(timeout = 20000)
  public void test_5_DepthIterExceptionReturn() {
    final int depth = 5;
    invokeBatchExceptionTest(depth, IterServer.getTestReturnException());
    invokeBatchExceptionTest(depth, IterServer.getTestReturnExceptionAfterSpecReturn());
  }

  private void invokeBatchExceptionTest(final int depth, final RpcSignature methodSignature) {
    invokeExceptionTest(depth, methodSignature, IterClient.TEST_REQUEST_VALUE);
    invokeExceptionTest(depth, methodSignature, IterClient.TEST_REQUEST_VALUE, IterClient.INCORRECT_SPEC_VALUE);
    invokeExceptionTest(depth, methodSignature, IterClient.TEST_REQUEST_VALUE, IterClient.CORRECT_SPEC_VALUE);
    invokeExceptionTest(depth, methodSignature, IterClient.TEST_REQUEST_VALUE, IterClient.CORRECT_SPEC_VALUE,
        IterClient.INCORRECT_SPEC_VALUE);
  }

  private void invokeExceptionTest(final int depth, final RpcSignature methodSignature, final String requestValue,
      Object... predictedValues) {
    try {
      ArrayList<Object> speculativeValues = new ArrayList<Object>();
      for (Object value : predictedValues) {
        speculativeValues.add(value);
      }
      client.testIterCallPattern(
          new IterClientExceptionCallbackFactory(depth, methodSignature, requestValue, speculativeValues),
          IterTest.SERVER_IDENTITY, methodSignature, requestValue, speculativeValues);
      Assert.assertEquals(true, false);// should not arrive here
    } catch (UserException e) {
      Assert.assertEquals(IterClient.EXPECTED_EXCEPTION, e.getMessage());
    }
  }

  // test exception return
  // first call does not get exception
  @Test(timeout = 20000)
  public void test_2_DepthIterExceptionReturnFirstCallNot() {
    final int depth = 2;
    invokeDifferentFirstCallExceptionTest(depth, IterServer.getTestReturnException());
    invokeDifferentFirstCallExceptionTest(depth, IterServer.getTestReturnExceptionAfterSpecReturn());
  }

  @Test(timeout = 20000)
  public void test_3_DepthIterExceptionReturnFirstCallNot() {
    final int depth = 3;
    invokeDifferentFirstCallExceptionTest(depth, IterServer.getTestReturnException());
    invokeDifferentFirstCallExceptionTest(depth, IterServer.getTestReturnExceptionAfterSpecReturn());
  }

  @Test(timeout = 20000)
  public void test_4_DepthIterExceptionReturnFirstCallNot() {
    final int depth = 4;
    invokeDifferentFirstCallExceptionTest(depth, IterServer.getTestReturnException());
    invokeDifferentFirstCallExceptionTest(depth, IterServer.getTestReturnExceptionAfterSpecReturn());
  }

  @Test(timeout = 20000)
  public void test_5_DepthIterExceptionReturnFirstCallNot() {
    final int depth = 5;
    invokeDifferentFirstCallExceptionTest(depth, IterServer.getTestReturnException());
    invokeDifferentFirstCallExceptionTest(depth, IterServer.getTestReturnExceptionAfterSpecReturn());
  }

  private void invokeDifferentFirstCallExceptionTest(final int depth, final RpcSignature methodSignature) {
    invokeBatchFirstCallNotExceptionTest(depth, IterServer.getTestReturnValue(), methodSignature);
    invokeBatchFirstCallNotExceptionTest(depth, IterServer.getTestCorrectSpecReturnValue(), methodSignature);
    invokeBatchFirstCallNotExceptionTest(depth, IterServer.getTestIncorrectSpecReturnValue(), methodSignature);
    invokeBatchFirstCallNotExceptionTest(depth, IterServer.getTestMultiIncorrectSpecReturnValue(), methodSignature);
    invokeBatchFirstCallNotExceptionTest(depth, IterServer.getTestMultiSpecReturnValueWithCorrectSpec(),
        methodSignature);
    invokeBatchFirstCallNotExceptionTest(depth, IterServer.getTestSpecBlockReturnValue(), methodSignature);
    invokeBatchFirstCallNotExceptionTest(depth, IterServer.getTestSpecBlockBeforeAnyReturn(), methodSignature);
    invokeBatchFirstCallNotExceptionTest(depth, IterServer.getTestSpecBlockCorrectSpecReturnValue(), methodSignature);
    invokeBatchFirstCallNotExceptionTest(depth, IterServer.getTestSpecBlockIncorrectSpecReturnValue(), methodSignature);
    invokeBatchFirstCallNotExceptionTest(depth, IterServer.getTestSpecBlockMultiIncorrectSpecReturnValue(),
        methodSignature);
    invokeBatchFirstCallNotExceptionTest(depth, IterServer.getTestSpecBlockMultiSpecReturnValueWithCorrectSpec(),
        methodSignature);

  }

  private void invokeBatchFirstCallNotExceptionTest(final int depth, final RpcSignature firstMethodSignature,
      final RpcSignature methodSignature) {
    invokeFirstCallNotExceptionTest(depth, firstMethodSignature, methodSignature, IterClient.TEST_REQUEST_VALUE);
    invokeFirstCallNotExceptionTest(depth, firstMethodSignature, methodSignature, IterClient.TEST_REQUEST_VALUE,
        IterClient.INCORRECT_SPEC_VALUE);
    invokeFirstCallNotExceptionTest(depth, firstMethodSignature, methodSignature, IterClient.TEST_REQUEST_VALUE,
        IterClient.CORRECT_SPEC_VALUE);
    invokeFirstCallNotExceptionTest(depth, firstMethodSignature, methodSignature, IterClient.TEST_REQUEST_VALUE,
        IterClient.CORRECT_SPEC_VALUE, IterClient.INCORRECT_SPEC_VALUE);
  }

  private void invokeFirstCallNotExceptionTest(final int depth, final RpcSignature firstMethodSignature,
      final RpcSignature methodSignature, final String requestValue, Object... predictedValues) {
    try {
      final String expected = IterClient.CALLBACK_RESULT_PREFIX + depth + " " + IterClient.EXPECTED_EXCEPTION;
      ArrayList<Object> speculativeValues = new ArrayList<Object>();
      for (Object value : predictedValues) {
        speculativeValues.add(value);
      }
      String actual = client.testIterCallPattern(
          new IterClientPreExceptionCallbackFactory(depth, methodSignature, requestValue, speculativeValues),
          IterTest.SERVER_IDENTITY, firstMethodSignature, requestValue, speculativeValues);
      Assert.assertEquals(expected, actual);// should not arrive here
    } catch (UserException e) {
      e.printStackTrace();
    }
  }

}
