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

import Waterloo.MultiSocket.exception.ConnectionCloseException;
import Waterloo.MultiSocket.exception.MultiSocketValidException;

import rpc.execption.MethodNotRegisteredException;
import rpc.execption.NoClientStubException;
import rpc.execption.UserException;
import specrpc.client.api.SpecRpcCallbackFactory;
import specrpc.client.api.SpecRpcFuture;
import specrpc.client.api.SpecRpcClientStub;
import specrpc.common.RpcSignature;
import specrpc.exception.SpeculationFailException;
import specrpc.server.api.SpecRpcHostObject;

public class ChainIterServiceHost extends SpecRpcHostObject {

  public static final String FIRST_MULTI_SPEC_RETURN = "firstMultiSpecReturn";
  public static final String SECOND_MULTI_SPEC_RETURN = "secondMultiSpecReturn";
  public static final String THIRD_MULTI_SPEC_RETURN = "thirdMultiSpecReturn";
  public static final String END_MULTI_SPEC_RETURN = "endMultiSpecReturn";

  public static final String FIRST_CLIENT_SPEC = "firstClientSpec";
  public static final String SECOND_CLIENT_SPEC = "secondClientSpec";
  public static final String THIRD_CLIENT_SPEC = "thirdClientSpec";
  public static final String END_RETURN = "endReturn";

  public static final String FIRST_MID_SERVER_SPEC = "firstMidServerSpec";
  public static final String SECOND_MID_SERVER_SPEC = "secondMidServerSpec";
  public static final String THIRD_MID_SERVER_SPEC = "thirdMidServerSpec";

  public static final String RESPONSE_VALUE_PREFIX = "Response ";
  public static final String INCORRECT_SPEC_VALE_PREFIX_1 = "Speculative Response 1st ";
  public static final String INCORRECT_SPEC_VALE_PREFIX_2 = "Speculative Response 2nd ";
  public static final String INCORRECT_SPEC_VALE_PREFIX_3 = "Speculative Response 3rd ";
  public static final String INCORRECT_SPEC_VALE_PREFIX_4 = "Speculative Response 4th ";

  public static final String CORRECT_SPEC_PREFIX = ChainIterServiceHost.RESPONSE_VALUE_PREFIX;
  public static final String INCORRECT_SPEC_VALUE = "Incorrect Server Spec Value";

  public static final int MULTI_SPEC_RETURN_SERVER_ITER_DEPTH = 2;
  public static final int CLIENT_SPEC_SERVER_ITER_DEPTH = 2;
  public static final int MID_SERVER_SPEC_SERVER_ITER_DEPTH = 2;

  public static final int WORK_TIME = 5;// ms

  public ChainIterServiceHost() {

  }

  private void doWork() {
    try {
      Thread.sleep(WORK_TIME);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  // chain end server's methods
  public String endReturn(String requestValue) {
    String result = ChainIterServiceHost.RESPONSE_VALUE_PREFIX + requestValue;
    return result;
  }

  public String endMultiSpecReturn(String requestValue) {
    String result = ChainIterServiceHost.RESPONSE_VALUE_PREFIX + requestValue;
    try {
      this.specRPCFacade.specReturn(ChainIterServiceHost.INCORRECT_SPEC_VALE_PREFIX_1 + requestValue);
      this.specRPCFacade.specReturn(result);// correct spec
      this.doWork();
      this.specRPCFacade.specReturn(ChainIterServiceHost.INCORRECT_SPEC_VALE_PREFIX_3 + requestValue);
    } catch (NoClientStubException | IOException | MultiSocketValidException | ConnectionCloseException
        | InterruptedException e) {
      e.printStackTrace();
    } catch (SpeculationFailException e) {
      ;
    }

    return result;
  }

  // chain first server's methods
  public String firstMultiSpecReturn(String requestValue) {
    boolean isSpec = false;
    return invokeIterMultiSpecReturn(requestValue, ChainIterServiceHost.MULTI_SPEC_RETURN_SERVER_ITER_DEPTH,
        ChainIterServer.getSecondMultiSpecReturn(), isSpec) + "";
  }

  // chain second server's methods
  public String secondMultiSpecReturn(String requestValue) {
    boolean isSpec = false;
    return invokeIterMultiSpecReturn(requestValue, ChainIterServiceHost.MULTI_SPEC_RETURN_SERVER_ITER_DEPTH,
        ChainIterServer.getThirdMultiSpecReturn(), isSpec) + "";
  }

  // chain third server's methods
  public String thirdMultiSpecReturn(String requestValue) {
    boolean isSpec = false;
    return invokeIterMultiSpecReturn(requestValue, ChainIterServiceHost.MULTI_SPEC_RETURN_SERVER_ITER_DEPTH,
        ChainIterServer.getEndMultiSpecReturn(), isSpec) + "";
  }

  // test client spec
  // chain first server's methods
  public String firstClientSpec(String requestValue) {
    boolean isSpec = false;
    return invokeIterMultiSpecReturn(requestValue, ChainIterServiceHost.CLIENT_SPEC_SERVER_ITER_DEPTH,
        ChainIterServer.getSecondClientSpec(), isSpec) + "";
  }

  // chain second server's methods
  public String secondClientSpec(String requestValue) {
    boolean isSpec = false;
    return invokeIterMultiSpecReturn(requestValue, ChainIterServiceHost.CLIENT_SPEC_SERVER_ITER_DEPTH,
        ChainIterServer.getThirdClientSpec(), isSpec) + "";
  }

  // chain third server's methods
  public String thirdClientSpec(String requestValue) {
    boolean isSpec = false;
    return invokeIterMultiSpecReturn(requestValue, ChainIterServiceHost.CLIENT_SPEC_SERVER_ITER_DEPTH,
        ChainIterServer.getEndReturn(), isSpec) + "";
  }

  // test mid server spec
  // chain first server's methods
  public String firstMidServerSpec(String requestValue) {
    boolean isSpec = false;
    return invokeIterMultiSpecReturn(requestValue, ChainIterServiceHost.MID_SERVER_SPEC_SERVER_ITER_DEPTH,
        ChainIterServer.getSecondMidServerSpec(), isSpec) + "";
  }

  // chain second server's methods
  public String secondMidServerSpec(String requestValue) {
    boolean isSpec = true;
    return invokeIterMultiSpecReturn(requestValue, ChainIterServiceHost.MID_SERVER_SPEC_SERVER_ITER_DEPTH,
        ChainIterServer.getThirdMidServerSpec(), isSpec) + "";
  }

  // chain third server's methods
  public String thirdMidServerSpec(String requestValue) {
    boolean isSpec = false;
    return invokeIterMultiSpecReturn(requestValue, ChainIterServiceHost.MID_SERVER_SPEC_SERVER_ITER_DEPTH,
        ChainIterServer.getEndReturn(), isSpec) + "";
  }

  private Object invokeIterMultiSpecReturn(String requestValue, final int depth, RpcSignature methodSignature,
      final boolean isSpec) {
    if (isSpec) {
      String incorrectSpec = ChainIterServiceHost.INCORRECT_SPEC_VALUE;
      return this.invokeRPC(requestValue, methodSignature,
          new ChainIterServerCallbackFactory(depth, methodSignature, requestValue, incorrectSpec), incorrectSpec);
    } else {
      return this.invokeRPC(requestValue, methodSignature,
          new ChainIterServerCallbackFactory(depth, methodSignature, requestValue));
    }
  }

  private Object invokeRPC(String requestValue, RpcSignature methodSignature, SpecRpcCallbackFactory factory,
      Object... predictedValues) {

    SpecRpcClientStub srvStub;
    try {
      srvStub = this.specRPCFacade.bind(ChainIterTest.SERVER_IDENTITY, methodSignature);

      ArrayList<Object> specValues = new ArrayList<Object>();
      for (Object value : predictedValues) {
        specValues.add(value);
      }
      SpecRpcFuture future = srvStub.call(specValues, factory, requestValue);
      return future.getResult();
    } catch (MethodNotRegisteredException | IOException | InterruptedException | UserException e) {
      e.printStackTrace();
    } catch (SpeculationFailException e) {
      ;
    }
    // exception happens goes here
    return null;
  }

}
