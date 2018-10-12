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

package specrpc.iterativeMultiServers;

import static org.junit.Assert.*;

import java.io.IOException;

import specrpc.client.api.SpecRpcStatistics;
import specrpc.common.RpcSignature;

public class IterMultiServersTest {

  public static final Long DEFAULT_LOCAL_CAL_TIME = 0l;// ms local cal time in call back
  public static final Long DEFAULT_SERVER_CAL_TIME = 0l;// ms rpc cal time in server side

  public static final String SERVER = "server";
  public static final String BLOCK_ANT = "block_ant";
  public static final String CLIENT = "client";

  /*
   * args[0] --> determines server or client args[1] --> serverID or clientID
   * args[2] --> number of clients, each client will start after previous client
   * stops
   * 
   * Note: running this test case, must make sure the server' pool size is large
   * enough (configured in rpc-defaults.conf) to handle the client's transactions,
   * otherwise client may block if there is not enough thread in server side to
   * deal with client's recursively calls.
   */
  public static void main(String args[]) throws InterruptedException, IOException {
    if (args.length < 2) {
      System.out.println("Invalid Arguments");
      return;
    }

    if (args[0].equals(SERVER)) {
      startServer(args[1]);
    } else if (args[0].equals(CLIENT)) {
      if (args.length < 3) {
        System.out.println("Invalid Args for Client simulation");
        return;
      }
      for (int i = 0; i < Integer.parseInt(args[2]); i++) {

        startClient((Integer.parseInt(args[1]) + i) + "");
        System.out.println("Client " + (Integer.parseInt(args[1]) + i) + " Finished");
        Thread.sleep(1000);// waits for a while to start next client
      }
      System.out.println("All clients end");
      System.out.println("Predictions total = " + SpecRpcStatistics.getTotalPredictionNumber() + "\n" + "correct = "
          + SpecRpcStatistics.getCorrectPredictionNumber() + "\n" + "incorrect = "
          + SpecRpcStatistics.getIncorrectPredictionNumber() + "\n");
    } else if (args[0].equals(BLOCK_ANT)) {
      // for ant script running server
      System.in.read();
    } else {
      System.out.println("Invalid Arguments");
      return;
    }
  }

  private static void startServer(String serverID) {
    IterMockServer server = new IterMockServer(serverID);
    Thread serverThread = new Thread(server);
    serverThread.start();
  }

  // client calls transactions
  private static void startClient(String clientID) {
    IterMockClient client = new IterMockClient(clientID);
    System.out.println("Client " + clientID + " starts");
    String result = null;

    // only test continuous put
    result = client.doTransaction(testPut(1), IterMockServer.PUT_PREDICT_RANDOM_VALUE);
    assertEquals(result, "true");
    System.out.println("continuous put 1 test finishes");

    result = client.doTransaction(testPut(2), IterMockServer.PUT_PREDICT_RANDOM_VALUE);
    assertEquals(result, "true:true");
    System.out.println("continuous put 2 test finishes");

    result = client.doTransaction(testPut(3), IterMockServer.PUT_PREDICT_RANDOM_VALUE);
    assertEquals(result, "true:true:true");
    System.out.println("continuous put 3 test finishes");

    result = client.doTransaction(testPut(4), IterMockServer.PUT_PREDICT_RANDOM_VALUE);
    assertEquals(result, "true:true:true:true");
    System.out.println("continuous put 4 test finishes");

    result = client.doTransaction(testPut(5), IterMockServer.PUT_PREDICT_RANDOM_VALUE);
    assertEquals(result, "true:true:true:true:true");
    System.out.println("continuous put 5 test finishes");

    result = client.doTransaction(testPut(6), IterMockServer.PUT_PREDICT_RANDOM_VALUE);
    assertEquals(result, "true:true:true:true:true:true");
    System.out.println("continuous put 6 test finishes");

    result = client.doTransaction(testPut(7), IterMockServer.PUT_PREDICT_RANDOM_VALUE);
    assertEquals(result, "true:true:true:true:true:true:true");
    System.out.println("continuous put 7 test finishes");

    result = client.doTransaction(testPut(8), IterMockServer.PUT_PREDICT_RANDOM_VALUE);
    assertEquals(result, "true:true:true:true:true:true:true:true");
    System.out.println("continuous put 8 test finishes");

    result = client.doTransaction(testPut(9), IterMockServer.PUT_PREDICT_RANDOM_VALUE);
    assertEquals(result, "true:true:true:true:true:true:true:true:true");
    System.out.println("continuous put 9 test finishes");

    result = client.doTransaction(testPut(10), IterMockServer.PUT_PREDICT_RANDOM_VALUE);
    assertEquals(result, "true:true:true:true:true:true:true:true:true:true");
    System.out.println("continuous put 10 test finishes");

    result = client.doTransaction(testPut(1), IterMockServer.PUT_PREDICT_ALWAYS_FALSE);
    assertEquals(result, "true");
    System.out.println("continuous put 1 test finishes");

    result = client.doTransaction(testPut(2), IterMockServer.PUT_PREDICT_ALWAYS_FALSE);
    assertEquals(result, "true:true");
    System.out.println("continuous put 2 test finishes");

    result = client.doTransaction(testPut(3), IterMockServer.PUT_PREDICT_ALWAYS_FALSE);
    assertEquals(result, "true:true:true");
    System.out.println("continuous put 3 test finishes");

    result = client.doTransaction(testPut(4), IterMockServer.PUT_PREDICT_ALWAYS_FALSE);
    assertEquals(result, "true:true:true:true");
    System.out.println("continuous put 4 test finishes");

    result = client.doTransaction(testPut(5), IterMockServer.PUT_PREDICT_ALWAYS_FALSE);
    assertEquals(result, "true:true:true:true:true");
    System.out.println("continuous put 5 test finishes");

    result = client.doTransaction(testPut(6), IterMockServer.PUT_PREDICT_ALWAYS_FALSE);
    assertEquals(result, "true:true:true:true:true:true");
    System.out.println("continuous put 6 test finishes");

    result = client.doTransaction(testPut(7), IterMockServer.PUT_PREDICT_ALWAYS_FALSE);
    assertEquals(result, "true:true:true:true:true:true:true");
    System.out.println("continuous put 7 test finishes");

    result = client.doTransaction(testPut(8), IterMockServer.PUT_PREDICT_ALWAYS_FALSE);
    assertEquals(result, "true:true:true:true:true:true:true:true");
    System.out.println("continuous put 8 test finishes");

    result = client.doTransaction(testPut(9), IterMockServer.PUT_PREDICT_ALWAYS_FALSE);
    assertEquals(result, "true:true:true:true:true:true:true:true:true");
    System.out.println("continuous put 9 test finishes");

    result = client.doTransaction(testPut(10), IterMockServer.PUT_PREDICT_ALWAYS_FALSE);
    assertEquals(result, "true:true:true:true:true:true:true:true:true:true");
    System.out.println("continuous put 10 test finishes");

    result = client.doTransaction(testPut(1), IterMockServer.PUT_PREDICT_ALWAYS_TRUE);
    assertEquals(result, "true");
    System.out.println("continuous put 1 test finishes");

    result = client.doTransaction(testPut(2), IterMockServer.PUT_PREDICT_ALWAYS_TRUE);
    assertEquals(result, "true:true");
    System.out.println("continuous put 2 test finishes");

    result = client.doTransaction(testPut(3), IterMockServer.PUT_PREDICT_ALWAYS_TRUE);
    assertEquals(result, "true:true:true");
    System.out.println("continuous put 3 test finishes");

    result = client.doTransaction(testPut(4), IterMockServer.PUT_PREDICT_ALWAYS_TRUE);
    assertEquals(result, "true:true:true:true");
    System.out.println("continuous put 4 test finishes");

    result = client.doTransaction(testPut(5), IterMockServer.PUT_PREDICT_ALWAYS_TRUE);
    assertEquals(result, "true:true:true:true:true");
    System.out.println("continuous put 5 test finishes");

    result = client.doTransaction(testPut(6), IterMockServer.PUT_PREDICT_ALWAYS_TRUE);
    assertEquals(result, "true:true:true:true:true:true");
    System.out.println("continuous put 6 test finishes");

    result = client.doTransaction(testPut(7), IterMockServer.PUT_PREDICT_ALWAYS_TRUE);
    assertEquals(result, "true:true:true:true:true:true:true");
    System.out.println("continuous put 7 test finishes");

    result = client.doTransaction(testPut(8), IterMockServer.PUT_PREDICT_ALWAYS_TRUE);
    assertEquals(result, "true:true:true:true:true:true:true:true");
    System.out.println("continuous put 8 test finishes");

    result = client.doTransaction(testPut(9), IterMockServer.PUT_PREDICT_ALWAYS_TRUE);
    assertEquals(result, "true:true:true:true:true:true:true:true:true");
    System.out.println("continuous put 9 test finishes");

    result = client.doTransaction(testPut(10), IterMockServer.PUT_PREDICT_ALWAYS_TRUE);
    assertEquals(result, "true:true:true:true:true:true:true:true:true:true");
    System.out.println("continuous put 10 test finishes");

    // ----------------Finish PUT Test-------------------------

    // Special Test
    result = client.doTransaction(test_4_Get_1_Write(), IterMockServer.PUT_PREDICT_ALWAYS_TRUE);
    assertEquals(result, "0:1:2:3:true");
    System.out.println("test_4_Get_1_Write - PREDICT_ALWAYS_TRUE - finishes");

    result = client.doTransaction(test_4_Get_1_Write(), IterMockServer.PUT_PREDICT_ALWAYS_FALSE);
    assertEquals(result, "0:1:2:3:true");
    System.out.println("test_4_Get_1_Write - PREDICT_ALWAYS_FALSE - finishes");

    result = client.doTransaction(test_4_Get_1_Write(), IterMockServer.PUT_PREDICT_RANDOM_VALUE);
    assertEquals(result, "0:1:2:3:true");
    System.out.println("test_4_Get_1_Write - PREDICT_RANDOM_VALUE - finishes");

    // ----------------Finish Special Test-------------------------

    // Standard Test
    // test continuous put
    result = client.doTransaction(testPut(10), IterMockServer.PUT_PREDICT_ALWAYS_TRUE);
    assertEquals(result, "true:true:true:true:true:true:true:true:true:true");
    System.out.println("continuous put 1 test finishes");

    // test continuous put
    result = client.doTransaction(testPut(10), IterMockServer.PUT_PREDICT_ALWAYS_FALSE);
    assertEquals(result, "true:true:true:true:true:true:true:true:true:true");
    System.out.println("continuous put 2 test finishes");

    // test continuous put
    result = client.doTransaction(testPut(10), IterMockServer.PUT_PREDICT_RANDOM_VALUE);
    assertEquals(result, "true:true:true:true:true:true:true:true:true:true");
    System.out.println("continuous put 3 test finishes");

    // test continuous get
    result = client.doTransaction(testGet(10), IterMockServer.PUT_PREDICT_RANDOM_VALUE);
    assertEquals(result, "0:1:2:3:4:5:6:7:8:9");
    System.out.println("continuous get 1 test finishes");

    // test (put,get) pair with updating the previous put values
    result = client.doTransaction(testPutGet(5), IterMockServer.PUT_PREDICT_RANDOM_VALUE);
    assertEquals(result, "true:1:true:3:true:5:true:7:true:9");
    System.out.println("continuous put/get 1 test finishes");

    // test (get,put) pair with updating the previous put values
    result = client.doTransaction(testGetPut(5), IterMockServer.PUT_PREDICT_RANDOM_VALUE);
    assertEquals(result, "10:true:12:true:14:true:16:true:18:true");
    System.out.println("continuous get/put 1 test finishes");

    // test continuous get
    result = client.doTransaction(testGet(10), IterMockServer.PUT_PREDICT_RANDOM_VALUE);
    assertEquals(result, "10:11:12:13:14:15:16:17:18:19");
    System.out.println("continuous get 2 test finishes");

  }

  // generating transactions for test cases
  // continuous specific times put in one operation
  private static Transaction testPut(int putTimes) {
    Transaction trans = new Transaction("testPut: " + putTimes + " continuous puts");

    ILocalCalculation localCal = new LocalCalculation(IterMultiServersTest.DEFAULT_LOCAL_CAL_TIME);

    for (int i = 0; i < putTimes; i++) {
      RpcSignature method = new RpcSignature(IterMockService.class.getName(), "putValue", Boolean.class,
          String.class, String.class, Long.class);

      Parameter arg = new Parameter();// args for RPC
      arg.addParameter(new Long(i));// put key
      arg.addParameter(new Long(i));// put value
      arg.addParameter(IterMultiServersTest.DEFAULT_SERVER_CAL_TIME);

      trans.addOperation(method, arg, localCal);
    }

    return trans;
  }

  // continuous specific times get in one operation
  private static Transaction testGet(final int getTimes) {
    Transaction trans = new Transaction("testGet: " + getTimes + " continuous gets");

    ILocalCalculation localCal = new LocalCalculation(IterMultiServersTest.DEFAULT_LOCAL_CAL_TIME);

    for (int i = 0; i < getTimes; i++) {
      RpcSignature method = new RpcSignature(IterMockService.class.getName(), "getValue", String.class,
          String.class, Long.class);

      Parameter arg = new Parameter();// args for RPC
      arg.addParameter(new Long(i));// get key
      arg.addParameter(IterMultiServersTest.DEFAULT_SERVER_CAL_TIME);// get cal time

      trans.addOperation(method, arg, localCal);
    }

    return trans;
  }

  // specific times of <put, get> pair, put & get calls different servers
  private static Transaction testPutGet(int put_getTimes) {
    Transaction trans = new Transaction("testPutGet: " + put_getTimes + " puts & gets alternatively");

    ILocalCalculation localCal = new LocalCalculation(IterMultiServersTest.DEFAULT_LOCAL_CAL_TIME);

    for (int i = 0; i < put_getTimes; i++) {
      RpcSignature putMethod = new RpcSignature(IterMockService.class.getName(), "putValue", Boolean.class,
          String.class, String.class, Long.class);

      Parameter putArg = new Parameter();// args for put RPC
      putArg.addParameter(new Long(i * 2));
      putArg.addParameter(new Long(i * 2 + 10));
      putArg.addParameter(IterMultiServersTest.DEFAULT_SERVER_CAL_TIME);

      trans.addOperation(putMethod, putArg, localCal);

      RpcSignature getMethod = new RpcSignature(IterMockService.class.getName(), "getValue", String.class,
          String.class, Long.class);

      Parameter getArg = new Parameter();// args for get RPC
      getArg.addParameter(new Long(i * 2 + 1));
      getArg.addParameter(IterMultiServersTest.DEFAULT_SERVER_CAL_TIME);

      trans.addOperation(getMethod, getArg, localCal);
    }

    return trans;
  }

  // specific times of <get, put> pair, put & get calls different servers
  private static Transaction testGetPut(int get_putTimes) {
    Transaction trans = new Transaction("testGetPut: " + get_putTimes + " gets & puts alternatively");

    ILocalCalculation localCal = new LocalCalculation(IterMultiServersTest.DEFAULT_LOCAL_CAL_TIME);

    for (int i = 0; i < get_putTimes; i++) {
      RpcSignature getMethod = new RpcSignature(IterMockService.class.getName(), "getValue", String.class,
          String.class, Long.class);

      Parameter getArg = new Parameter();// args for get RPC
      getArg.addParameter(new Long(i * 2));
      getArg.addParameter(IterMultiServersTest.DEFAULT_SERVER_CAL_TIME);
      trans.addOperation(getMethod, getArg, localCal);

      RpcSignature putMethod = new RpcSignature(IterMockService.class.getName(), "putValue", Boolean.class,
          String.class, String.class, Long.class);

      Parameter putArg = new Parameter();// args for put RPC
      putArg.addParameter(new Long(i * 2 + 1));
      putArg.addParameter(new Long(i * 2 + 1 + 10));
      putArg.addParameter(IterMultiServersTest.DEFAULT_SERVER_CAL_TIME);
      trans.addOperation(putMethod, putArg, localCal);
    }

    return trans;
  }

  private static Transaction test_4_Get_1_Write() {
    Transaction trans = new Transaction("test_4_Get_1_Write");

    ILocalCalculation localCal = new LocalCalculation(IterMultiServersTest.DEFAULT_LOCAL_CAL_TIME);
    int i = 0;
    for (; i < 4; i++) {
      RpcSignature getMethod = new RpcSignature(IterMockService.class.getName(), "getValue", String.class,
          String.class, Long.class);
      Parameter getArg = new Parameter();// args for get RPC
      getArg.addParameter(new Long(i));
      getArg.addParameter(IterMultiServersTest.DEFAULT_SERVER_CAL_TIME);
      trans.addOperation(getMethod, getArg, localCal);
    }
    RpcSignature putMethod = new RpcSignature(IterMockService.class.getName(), "putValue", Boolean.class,
        String.class, String.class, Long.class);

    Parameter putArg = new Parameter();// args for put RPC
    putArg.addParameter(new Long(i));
    putArg.addParameter(new Long(i));
    putArg.addParameter(IterMultiServersTest.DEFAULT_SERVER_CAL_TIME);
    trans.addOperation(putMethod, putArg, localCal);
    return trans;
  }
}
