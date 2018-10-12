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

import java.util.ArrayList;

import junit.framework.Assert;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

import rpc.execption.UserException;
import specrpc.client.api.SpecRpcStatistics;
import specrpc.common.RpcSignature;

public class ChainIterTest {

  public static final String SERVER_IDENTITY = "ChainIterTestServer-ID";

  private static ChainIterServer server = new ChainIterServer(ChainIterTest.SERVER_IDENTITY);
  private static ChainIterClient client = new ChainIterClient();
  private static boolean serverStarted = false;

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

  // end server multi spec return

  @Test(timeout = 20000)
  public void testClientSpec() {
    final int clientIterDepth = 2;
    invokeTest(clientIterDepth, ChainIterServiceHost.CLIENT_SPEC_SERVER_ITER_DEPTH,
        ChainIterServer.getFirstClientSpec(), true);
  }

  @Test(timeout = 20000)
  public void testMidServerSpec() {
    final int clientIterDepth = 2;
    invokeTest(clientIterDepth, ChainIterServiceHost.MID_SERVER_SPEC_SERVER_ITER_DEPTH,
        ChainIterServer.getFirstMidServerSpec(), false);
  }

  @Test(timeout = 20000)
  public void testEndServerMultiSpecReturn() {
    final int clientIterDepth = 2;
    invokeTest(clientIterDepth, ChainIterServiceHost.MULTI_SPEC_RETURN_SERVER_ITER_DEPTH,
        ChainIterServer.getFirstMultiSpecReturn(), false);
  }

  private void invokeTest(final int clientIterDepth, final int serverIterDepth, RpcSignature methodSignature,
      boolean isSpec) {

    ArrayList<Object> predictedValues = new ArrayList<Object>();
    if (isSpec) {
      predictedValues.add(ChainIterClient.INCORRECT_SPEC_VALUE);
    }

    String result = null;
    try {
      result = client.testChainIterCallPattern(
          new ChainIterClientCallbackFactory(clientIterDepth, methodSignature, ChainIterClient.TEST_REQUEST_VALUE,
              predictedValues),
          ChainIterTest.SERVER_IDENTITY, methodSignature, ChainIterClient.TEST_REQUEST_VALUE, predictedValues);
    } catch (UserException e) {
      e.printStackTrace();
    }

    String[] checkingResults = result.split(" ");
    for (int i = 0; i < checkingResults.length - 2; i++) {
      Assert.assertEquals("Response", checkingResults[i]);
    }
    Assert.assertEquals("Request", checkingResults[checkingResults.length - 2]);
    Assert.assertEquals("Value", checkingResults[checkingResults.length - 1]);
    Assert.assertEquals(serverIterDepth * serverIterDepth * serverIterDepth * clientIterDepth,
        checkingResults.length - 2);
  }
}
