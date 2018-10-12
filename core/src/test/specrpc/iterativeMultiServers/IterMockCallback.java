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
import specrpc.client.api.SpecRpcCallbackObject;
import specrpc.client.api.SpecRpcFuture;
import specrpc.client.api.SpecRpcClientStub;
import specrpc.common.RpcSignature;
import specrpc.exception.SpeculationFailException;

public class IterMockCallback extends SpecRpcCallbackObject {

  private final String clientID;
  private final Transaction tran;
  private final int currentOpIndex;// index of the next operation in transaction
  private final String putPredictTag;// determine how putValue predicts; defined in IterMockServer

  public IterMockCallback(String id, Transaction tran, int currentOpIndex, String putPredictTag) {
    this.clientID = id;
    this.tran = tran;
    this.currentOpIndex = currentOpIndex;
    this.putPredictTag = putPredictTag;
  }

  @Override
  public Object run(Object rpcReturnValue) throws SpeculationFailException, InterruptedException {
    int previousOpIndex = this.currentOpIndex - 1;
    this.tran.getLocalCals().get(previousOpIndex).calculate("NULL");

    // calculate result according to rpcReturnValue
    String transactionResult = "";

    if (rpcReturnValue != null) {
      // record the rpcReturnValue into transactionResult
      transactionResult += rpcReturnValue.toString();
    } else {
      if (this.tran.getOperations().get(previousOpIndex).methodName.equals(IterMockClient.GET_VALUE)) {
        transactionResult += "NULL";
      } else if (this.tran.getOperations().get(previousOpIndex).methodName.equals(IterMockClient.PUT_VALUE)) {
        transactionResult += new Boolean(false).toString();
      } else {
        // should not execute here
        System.out.println("IterMockCallback : Transaction is running in a wrong way...");
      }
    }

    // next rpc if applicable
    if (this.currentOpIndex < this.tran.getOperations().size()) {
      // more operations in this transaction
      RpcSignature method = this.tran.getOperation(this.currentOpIndex);

      try {
        // next operation in transaction
        SpecRpcClientStub srvStub = this.specRPCFacade.bind(this.currentOpIndex + "", method);
        List<Object> predictedValues = new ArrayList<Object>();

        if (method.methodName.equals(IterMockClient.GET_VALUE)) {
          // predict values according to the method signature
          // based on testTag to predict values
          if (putPredictTag.equals(IterMockServer.PUT_PREDICT_ALWAYS_FALSE)) {
            predictedValues.add("WRONG PREDICTION");
          } else if (putPredictTag.equals(IterMockServer.PUT_PREDICT_RANDOM_VALUE)) {
            predictedValues.add(new Random().nextInt() + "");
          } else {
            // IterMockServer.PUT_PREDICT_ALWAYS_TRUE
            if (this.currentOpIndex == 0)
              predictedValues.add("0");
            else if (this.currentOpIndex == 1)
              predictedValues.add("1");
            else if (this.currentOpIndex == 2)
              predictedValues.add("2");
            else if (this.currentOpIndex == 3)
              predictedValues.add("3");
          }
        } else if (method.methodName.equals(IterMockClient.PUT_VALUE)) {
          // based on testTag to predict values
          if (this.putPredictTag.equals(IterMockServer.PUT_PREDICT_ALWAYS_FALSE)) {
            predictedValues.add(new Boolean(false));
          } else if (this.putPredictTag.equals(IterMockServer.PUT_PREDICT_RANDOM_VALUE)) {
            predictedValues.add(new Random().nextBoolean());
          } else {
            // IterMockServer.PUT_PREDICT_ALWAYS_TRUE
            predictedValues.add(new Boolean(true));
          }
        } else {
          System.out.println("IterMockCallback : Unkown Operation " + method.methodName);
        }

        SpecRpcFuture future = srvStub.call(predictedValues,
            new IterMockCallbackFactory(this.clientID, this.tran, this.currentOpIndex + 1, this.putPredictTag),
            this.tran.getParameters().get(this.currentOpIndex).getParameters().toArray());

        transactionResult += IterMockServer.OPERATION_RESULT_SEPERATOR + future.getResult();

      } catch (MethodNotRegisteredException | IOException | UserException e) {
        e.printStackTrace();
      }
    }

    return transactionResult;
  }

}
