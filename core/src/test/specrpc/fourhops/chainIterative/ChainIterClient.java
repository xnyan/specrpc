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

import java.io.IOException;
import java.util.ArrayList;

import rpc.execption.MethodNotRegisteredException;
import rpc.execption.UserException;
import specrpc.client.api.SpecRpcCallbackFactory;
import specrpc.client.api.SpecRpcClient;
import specrpc.client.api.SpecRpcFuture;
import specrpc.client.api.SpecRpcClientStub;
import specrpc.common.RpcSignature;
import specrpc.exception.SpeculationFailException;

public class ChainIterClient extends Thread {

  public static final String TEST_REQUEST_VALUE = "Request Value";
  public static final String INCORRECT_SPEC_VALUE = "Client Incorrect Spec " + TEST_REQUEST_VALUE;

  public ChainIterClient() {
    try {
      SpecRpcClient.initClient(null);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public synchronized void terminate() {
    SpecRpcClient.shutdown();
  }

  public String testChainIterCallPattern(SpecRpcCallbackFactory callbackFactory, String serverIdentity,
      RpcSignature methodSignature, String requestValue, ArrayList<Object> predictedValues) throws UserException {
    try {
      SpecRpcClientStub srvStub = SpecRpcClient.bind(serverIdentity, methodSignature);
      SpecRpcFuture future = srvStub.call(predictedValues, callbackFactory, requestValue);
      return future.getResult().toString();
    } catch (MethodNotRegisteredException | IOException | SpeculationFailException | InterruptedException e) {
      e.printStackTrace();
    } catch (UserException e) {
      throw e;
    }

    return null;
  }
}
