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

import Waterloo.MultiSocket.exception.ConnectionCloseException;
import Waterloo.MultiSocket.exception.MultiSocketValidException;
import rpc.execption.NoClientStubException;
import specrpc.exception.SpeculationFailException;
import specrpc.server.api.SpecRpcHostObject;

public class OneHopServiceHost extends SpecRpcHostObject {

  public static final String TEST_ONLY_ACTUAL_RETURN = "testOnlyActualReturn";
  public static final String TEST_CORRECT_SPEC_RETURN = "testCorrectSpeculativeReturn";
  public static final String TEST_INCORRECT_SPEC_RETURN = "testIncorrectSpeculativeReturn";
  public static final String TEST_BOTH_CORRECT_INCORRECT_SPEC_RETURN = "testBothCorrectInCorrectSpecReturn";
  public static final String TEST_MULTIPLE_SPEC_RETURN = "testMultipleSpecReturn";
  public static final String TEST_EXCEPTION_RETURN = "testExceptionReturn";
  public static final String TEST_EXCEPTION_AFTER_SPEC_RETURN = "testEceptionAfterSpecReturn";

  public static final String RESPONSE_VALUE_PREFIX = "Response ";
  public static final String INCORRECT_SPEC_VALE_PREFIX_1 = "Speculative Response 1st ";
  public static final String INCORRECT_SPEC_VALE_PREFIX_2 = "Speculative Response 2nd ";
  public static final String INCORRECT_SPEC_VALE_PREFIX_3 = "Speculative Response 3rd ";
  public static final String INCORRECT_SPEC_VALE_PREFIX_4 = "Speculative Response 4th ";

  public static final String EXCEPTION_PREFIX = "Exception ";

  public static final long SLEEP_TIME = 20;

  public OneHopServiceHost() {

  }

  // only one actual return value
  public String testOnlyActualReturn(String requestValue) throws NoClientStubException, SpeculationFailException,
      InterruptedException, IOException, MultiSocketValidException, ConnectionCloseException {
    String responseValue = OneHopServiceHost.RESPONSE_VALUE_PREFIX + requestValue;

    Thread.sleep(SLEEP_TIME);
    return responseValue;
  }

  // one correct spec value & one actual return value
  public String testCorrectSpeculativeReturn(String requestValue) throws NoClientStubException,
      SpeculationFailException, InterruptedException, IOException, MultiSocketValidException, ConnectionCloseException {
    this.specRPCFacade.specReturn(OneHopServiceHost.RESPONSE_VALUE_PREFIX + requestValue);

    Thread.sleep(SLEEP_TIME);

    String responseValue = OneHopServiceHost.RESPONSE_VALUE_PREFIX + requestValue;
    return responseValue;
  }

  // one incorrect spec value & one actual return value
  public String testIncorrectSpeculativeReturn(String requestValue) throws NoClientStubException,
      SpeculationFailException, InterruptedException, IOException, MultiSocketValidException, ConnectionCloseException {
    this.specRPCFacade.specReturn(OneHopServiceHost.INCORRECT_SPEC_VALE_PREFIX_1 + requestValue);

    Thread.sleep(SLEEP_TIME);

    String responseValue = OneHopServiceHost.RESPONSE_VALUE_PREFIX + requestValue;
    return responseValue;
  }

  // one correct spec value, one incorrect spec value and one actual return value
  public String testBothCorrectInCorrectSpecReturn(String requestValue) throws NoClientStubException,
      SpeculationFailException, InterruptedException, IOException, MultiSocketValidException, ConnectionCloseException {
    this.specRPCFacade.specReturn(OneHopServiceHost.INCORRECT_SPEC_VALE_PREFIX_1 + requestValue);
    this.specRPCFacade.specReturn(OneHopServiceHost.RESPONSE_VALUE_PREFIX + requestValue);

    Thread.sleep(SLEEP_TIME);

    String responseValue = OneHopServiceHost.RESPONSE_VALUE_PREFIX + requestValue;
    return responseValue;
  }

  // one correct spec value, 4 incorrect spec values and one actual return value
  public String testMultipleSpecReturn(String requestValue) throws NoClientStubException, SpeculationFailException,
      InterruptedException, IOException, MultiSocketValidException, ConnectionCloseException {
    this.specRPCFacade.specReturn(OneHopServiceHost.INCORRECT_SPEC_VALE_PREFIX_1 + requestValue);
    this.specRPCFacade.specReturn(OneHopServiceHost.RESPONSE_VALUE_PREFIX + requestValue);
    this.specRPCFacade.specReturn(OneHopServiceHost.INCORRECT_SPEC_VALE_PREFIX_2 + requestValue);

    Thread.sleep(SLEEP_TIME);

    this.specRPCFacade.specReturn(OneHopServiceHost.INCORRECT_SPEC_VALE_PREFIX_3 + requestValue);
    this.specRPCFacade.specReturn(OneHopServiceHost.INCORRECT_SPEC_VALE_PREFIX_4 + requestValue);

    String responseValue = OneHopServiceHost.RESPONSE_VALUE_PREFIX + requestValue;
    return responseValue;
  }

  // test just one exception return
  public String testExceptionReturn(String requestValue) throws NoClientStubException, SpeculationFailException,
      InterruptedException, IOException, MultiSocketValidException, ConnectionCloseException {
    String responseException = OneHopServiceHost.EXCEPTION_PREFIX + requestValue;

    Thread.sleep(SLEEP_TIME);

    this.specRPCFacade.throwNonSpecExceptionToClient(responseException);

    return responseException;
  }

  // test one exception after incorrect return
  public String testEceptionAfterSpecReturn(String requestValue) throws NoClientStubException, SpeculationFailException,
      InterruptedException, IOException, MultiSocketValidException, ConnectionCloseException {
    this.specRPCFacade.specReturn(OneHopServiceHost.INCORRECT_SPEC_VALE_PREFIX_1 + requestValue);
    this.specRPCFacade.specReturn(OneHopServiceHost.INCORRECT_SPEC_VALE_PREFIX_2 + requestValue);

    Thread.sleep(SLEEP_TIME);

    this.specRPCFacade.specReturn(OneHopServiceHost.INCORRECT_SPEC_VALE_PREFIX_3 + requestValue);
    this.specRPCFacade.specReturn(OneHopServiceHost.INCORRECT_SPEC_VALE_PREFIX_4 + requestValue);

    String responseException = OneHopServiceHost.EXCEPTION_PREFIX + requestValue;
    this.specRPCFacade.throwNonSpecExceptionToClient(responseException);

    return responseException;
  }
}
