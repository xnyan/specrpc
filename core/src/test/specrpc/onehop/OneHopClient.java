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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import rpc.execption.MethodNotRegisteredException;
import rpc.execption.UserException;
import specrpc.client.api.SpecRpcClient;
import specrpc.client.api.SpecRpcFuture;
import specrpc.client.api.SpecRpcClientStub;
import specrpc.common.RpcSignature;
import specrpc.exception.SpeculationFailException;

public class OneHopClient {

  public static final String TEST_REQUEST_VALUE = "Request Value";
  public static final String CORRECT_SPEC_VALUE = OneHopServiceHost.RESPONSE_VALUE_PREFIX + TEST_REQUEST_VALUE;
  public static final String ONLY_CLIENT_INCORRECT_SPEC_VALUE = "Only Client Incorrect Spec " + TEST_REQUEST_VALUE;
  public static final String INCORRECT_SPEC_VALUE_1 = OneHopServiceHost.INCORRECT_SPEC_VALE_PREFIX_1
      + TEST_REQUEST_VALUE;
  public static final String INCORRECT_SPEC_VALUE_2 = OneHopServiceHost.INCORRECT_SPEC_VALE_PREFIX_2
      + TEST_REQUEST_VALUE;
  public static final String INCORRECT_SPEC_VALUE_3 = OneHopServiceHost.INCORRECT_SPEC_VALE_PREFIX_3
      + TEST_REQUEST_VALUE;
  public static final String INCORRECT_SPEC_VALUE_4 = OneHopServiceHost.INCORRECT_SPEC_VALE_PREFIX_4
      + TEST_REQUEST_VALUE;

  private OneHopClientCallbackFactory callbackFactory;

  public OneHopClient() {
    try {
      SpecRpcClient.initClient(null);// initialize client-side SpecRPC framework
      this.callbackFactory = new OneHopClientCallbackFactory();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public synchronized void terminate() {
    SpecRpcClient.shutdown();
  }

  public synchronized OneHopClientCallbackFactory getCallbackFactory() {
    return this.callbackFactory;
  }

  public synchronized void setCallbackFactory(OneHopClientCallbackFactory factory) {
    this.callbackFactory = factory;
  }

  public String testReturnMethod(String serverIdentity, String methodName, String requestValue,
      Object... predictedValues) {
    RpcSignature rpcMethod = new RpcSignature(OneHopServiceHost.class.getName(), methodName, String.class,
        String.class);
    String callbackResult = null;
    try {
      callbackResult = invokeRPC(serverIdentity, rpcMethod, requestValue, predictedValues);
    } catch (UserException e) {
      e.printStackTrace();
    }
    return callbackResult;
  }

  public String testEception(String serverIdentity, String methodName, String requestValue, Object... predictedValues)
      throws UserException {
    RpcSignature rpcMethod = new RpcSignature(OneHopServiceHost.class.getName(), methodName, String.class,
        String.class);
    return invokeRPC(serverIdentity, rpcMethod, requestValue, predictedValues);
  }

  private String invokeRPC(String serverIdentity, RpcSignature rpcMethod, String requestValue,
      Object... predictedValues) throws UserException {
    String callbackResult = null;

    try {
      SpecRpcClientStub srvStub = SpecRpcClient.bind(serverIdentity, rpcMethod);

      List<Object> speculativeValues = new ArrayList<Object>();
      for (Object value : predictedValues) {
        speculativeValues.add(value);
      }

      SpecRpcFuture future = srvStub.call(speculativeValues, callbackFactory, requestValue);
      callbackResult = future.getResult().toString();

    } catch (MethodNotRegisteredException | IOException | SpeculationFailException | InterruptedException e) {
      e.printStackTrace();
    } catch (UserException e) {
      throw e;
    }

    return callbackResult;
  }
}