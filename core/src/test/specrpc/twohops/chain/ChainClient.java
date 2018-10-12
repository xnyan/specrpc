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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import rpc.execption.MethodNotRegisteredException;
import rpc.execption.UserException;
import specrpc.client.api.SpecRpcCallbackFactory;
import specrpc.client.api.SpecRpcClient;
import specrpc.client.api.SpecRpcFuture;
import specrpc.client.api.SpecRpcClientStub;
import specrpc.common.RpcSignature;
import specrpc.exception.SpeculationFailException;

public class ChainClient extends Thread {

  public static final String REQUEST_VALUE = "Request Value";
  public static final String INCORRECT_SPEC_VALUE = "Client Incorrect Spec Respone Value";
  public static final String CORRECT_SPEC_VALUE = ChainServiceMessages.MID_SERVER_RESPONSE_PREFIX
      + ChainServiceMessages.END_SERVER_RESPONSE_PREFIX + ChainClient.REQUEST_VALUE;;

  private SpecRpcCallbackFactory callbackFactory;

  public ChainClient() {
    try {
      SpecRpcClient.initClient(null);
      callbackFactory = new ChainClientCallbackFactory();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public synchronized SpecRpcCallbackFactory getFactory() {
    return this.callbackFactory;
  }

  public synchronized void setFactory(SpecRpcCallbackFactory callbackFactory) {
    this.callbackFactory = callbackFactory;
  }

  public synchronized void terminate() {
    SpecRpcClient.shutdown();
  }

  public synchronized String testReturnValue(String serverIdentity, String methodName, String requestValue,
      Object... predictedValues) throws UserException {
    RpcSignature rpcMethod = new RpcSignature(ChainServiceHost.class.getName(), methodName, String.class,
        String.class);
    String callbackResult = null;
    try {
      callbackResult = invokeRPC(serverIdentity, rpcMethod, requestValue, predictedValues);
    } catch (UserException e) {
      throw e;
    }
    return callbackResult;
  }

  private synchronized String invokeRPC(String serverIdentity, RpcSignature rpcMethod, String requestValue,
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

    } catch (MethodNotRegisteredException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    } catch (SpeculationFailException e) {
      e.printStackTrace();
    } catch (InterruptedException e) {
      e.printStackTrace();
    } catch (UserException e) {
      throw e;
    }

    return callbackResult;
  }
}
