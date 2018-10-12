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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import rpc.execption.MethodNotRegisteredException;
import rpc.execption.UserException;
import specrpc.client.api.SpecRpcCallbackFactory;
import specrpc.client.api.SpecRpcClient;
import specrpc.client.api.SpecRpcFuture;
import specrpc.client.api.SpecRpcClientStub;
import specrpc.common.RpcSignature;
import specrpc.exception.SpeculationFailException;

public class IterMockClient {

  public static final String TRANSACTION_RESULT_SEPERATOR = ">>";

  public static final String GET_VALUE = "getValue";
  public static final String PUT_VALUE = "putValue";

  private final String clientID;

  public IterMockClient(String id) {
    this.clientID = id;
    try {
      SpecRpcClient.initClient(null);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public String doTransaction(Transaction tran, String putPredictTag) {
    String transactionResult = null;

    try {
      // it is assumed that the sizes of operations and localCals are the same
      if (tran.getOperations().isEmpty()) {
        return "Invalid Transaction";
      }

      int currentOpIndex = 0;
      RpcSignature method = tran.getOperations().get(currentOpIndex);
      int nextOpIndex = currentOpIndex + 1;

      SpecRpcClientStub srvStub = SpecRpcClient.bind(currentOpIndex + "", method);
      List<Object> predictedValues = new ArrayList<Object>();
      SpecRpcCallbackFactory callbackFactory = new IterMockCallbackFactory(this.clientID, tran, nextOpIndex,
          putPredictTag);
      SpecRpcFuture future = null;

      if (method.methodName.equals(IterMockClient.GET_VALUE)) {
        // predict values according to method signature
        // based on testTag to predict values
        if (putPredictTag.equals(IterMockServer.PUT_PREDICT_ALWAYS_FALSE)) {
          predictedValues.add("WRONG PREDICTION");
        } else if (putPredictTag.equals(IterMockServer.PUT_PREDICT_RANDOM_VALUE)) {
          predictedValues.add(new Random().nextInt() + "");
        } else {
          // IterMockServer.PUT_PREDICT_ALWAYS_TRUE
          if (currentOpIndex == 0)
            predictedValues.add("0");
        }
      } else if (method.methodName.equals(IterMockClient.PUT_VALUE)) {
        // based on testTag to predict values
        if (putPredictTag.equals(IterMockServer.PUT_PREDICT_ALWAYS_FALSE)) {
          predictedValues.add(new Boolean(false));
        } else if (putPredictTag.equals(IterMockServer.PUT_PREDICT_RANDOM_VALUE)) {
          predictedValues.add(new Random().nextBoolean());
        } else {
          // IterMockServer.PUT_PREDICT_ALWAYS_TRUE
          predictedValues.add(new Boolean(true));
        }
      } else {
        System.out.println("Unkown Operation: " + method.methodName);
      }

      future = srvStub.call(predictedValues, callbackFactory, tran.getParameters().get(0).getParameters().toArray());

      transactionResult = future.getResult().toString();

    } catch (MethodNotRegisteredException | IOException | SpeculationFailException | InterruptedException | UserException e) {
      e.printStackTrace();
    }

    return transactionResult;
  }
}
