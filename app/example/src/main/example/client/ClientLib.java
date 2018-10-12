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

package example.client;

import java.io.IOException;
import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import example.server.CommitResult;
import example.server.Service;
import rpc.config.Constants;
import rpc.execption.MethodNotRegisteredException;
import rpc.execption.UserException;
import specrpc.client.api.SpecRpcClient;
import specrpc.client.api.SpecRpcFuture;
import specrpc.client.api.SpecRpcClientStub;
import specrpc.exception.SpeculationFailException;

public class ClientLib {

  private static final Logger logger = LoggerFactory.getLogger(Constants.LOGGER_TYPE);

  public ClientLib(String rpcConfigFile) {
    try {
      SpecRpcClient.initClient(rpcConfigFile);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public CommitResult commit(String txnId, String[] readKeyList, String[] writeKeyList, String[] writeValList) {
    CommitResult result = null;
    CommitResult incorrectPrediction = new CommitResult(txnId, true, 1.2f);
    CommitResult correctPrediction = new CommitResult(txnId, true, 0.2f);
    ArrayList<Object> predictions = new ArrayList<Object>();
    predictions.add(incorrectPrediction);
    predictions.add(correctPrediction);
    for (Object obj : predictions) {
      logger.info("Prediction: " + obj.toString());
    }
    try {
      SpecRpcClientStub rpcStub = SpecRpcClient.bind("1", Service.COMMIT);
      SpecRpcFuture future = rpcStub.call(predictions, new CommitCallbackFactory(), txnId, readKeyList, writeKeyList,
          writeValList);
      result = (CommitResult) future.getResult();
      logger.info("Execpted result: " + correctPrediction.toString() + " because of customized equivalence.");
      return (CommitResult) result;
    } catch (MethodNotRegisteredException | IOException | InterruptedException | UserException e) {
      e.printStackTrace();
    } catch (SpeculationFailException e) {

    }
    return result;
  }

  public void close() {
    SpecRpcClient.shutdown();
  }
}
