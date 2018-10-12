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

package specrpc.onehop.iterative;

import java.io.IOException;

import Waterloo.MultiSocket.exception.ConnectionCloseException;
import Waterloo.MultiSocket.exception.MultiSocketValidException;
import junit.framework.Assert;
import rpc.execption.NoClientStubException;
import specrpc.common.Status.SpeculationStatus;
import specrpc.exception.SpeculationFailException;
import specrpc.server.api.SpecRpcHostObject;

public class IterServiceHost extends SpecRpcHostObject {

  // host methods
  public static final String TEST_RETURN_VALUE = "testReturnValue";
  public static final String TEST_INCORRECT_SPEC_RETURN_VALUE = "testIncorrectSpecReturnValue";
  public static final String TEST_CORRECT_SPEC_RETURN_VALUE = "testCorrectSpecReturnValue";
  public static final String TEST_MULTI_INCORRECT_SPEC_RETURN_VALUE = "testMultiIncorrectSpecReturnValue";
  public static final String TEST_MULTI_SPEC_RETURN_VALUE_WITH_CORRECT_SPEC = "testMultiSpecReturnValueWithCorrectSpec";
  public static final String TEST_SPEC_BLOCK_RETURN_VALUE = "testSpecBlockReturnValue";
  public static final String TEST_SPEC_BLOCK_BEFORE_ANY_RETURN = "testSpecBlockBeforeAnyReturn";
  public static final String TEST_SPEC_BLOCK_INCORRECT_SPEC_RETURN_VALUE = "testSpecBlockIncorrectSpecReturnValue";
  public static final String TEST_SPEC_BLOCK_CORRECT_SPEC_RETURN_VALUE = "testSpecBlockCorrectSpecReturnValue";
  public static final String TEST_SPEC_BLOCK_MULTI_INCORRECT_SPEC_RETURN_VALUE = "testSpecBlockMultiIncorrectSpecReturnValue";
  public static final String TEST_SPEC_BLOCK_MULTI_SPEC_RETURN_VALUE_WITH_CORRECT_SPEC = "testSpecBlockMultiSpecReturnValueWithCorrectSpec";
  public static final String TEST_RETURN_EXCEPTION = "testReturnException";
  public static final String TEST_RETURN_EXCEPTION_AFTER_SPEC_RETURN = "testReturnExceptionAfterSpecReturn";

  public static final String RETURN_VALUE_PREFIX = "Response ";
  public static final String SPEC_RETURN_VALUE_PREFIX_1st = "Spec Response 1st ";
  public static final String SPEC_RETURN_VALUE_PREFIX_2nd = "Spec Response 2nd ";
  public static final String SPEC_RETURN_VALUE_PREFIX_3rd = "Spec Response 3rd ";
  public static final String SPEC_RETURN_VALUE_PREFIX_4th = "Spec Response 4th ";
  public static final String RETURN_EXCEPTION_PREFIX = "Exception ";

  public static final int WORK_TIME = 5;// ms

  public IterServiceHost() {

  }

  private void doWork() {
    try {
      Thread.sleep(IterServiceHost.WORK_TIME);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  public String testReturnValue(String requestValue) {
    String result = IterServiceHost.RETURN_VALUE_PREFIX + requestValue;
    this.doWork();
    return result;
  }

  public String testIncorrectSpecReturnValue(String requestValue) {
    String result = IterServiceHost.RETURN_VALUE_PREFIX + requestValue;
    try {
      this.specRPCFacade.specReturn(IterServiceHost.SPEC_RETURN_VALUE_PREFIX_1st + requestValue);
      this.doWork();
    } catch (NoClientStubException | InterruptedException | IOException | MultiSocketValidException
        | ConnectionCloseException e) {
      e.printStackTrace();
    } catch (SpeculationFailException e) {
      ;
    }
    return result;
  }

  public String testCorrectSpecReturnValue(String requestValue) {
    String result = IterServiceHost.RETURN_VALUE_PREFIX + requestValue;
    try {
      this.specRPCFacade.specReturn(result);
      this.doWork();
    } catch (NoClientStubException | InterruptedException | IOException | MultiSocketValidException
        | ConnectionCloseException e) {
      e.printStackTrace();
    } catch (SpeculationFailException e) {
      ;
    }
    return result;
  }

  public String testMultiIncorrectSpecReturnValue(String requestValue) {
    String result = IterServiceHost.RETURN_VALUE_PREFIX + requestValue;
    try {
      this.specRPCFacade.specReturn(IterServiceHost.SPEC_RETURN_VALUE_PREFIX_1st + requestValue);
      this.specRPCFacade.specReturn(IterServiceHost.SPEC_RETURN_VALUE_PREFIX_2nd + requestValue);
      this.doWork();
      this.specRPCFacade.specReturn(IterServiceHost.SPEC_RETURN_VALUE_PREFIX_3rd + requestValue);
      this.specRPCFacade.specReturn(IterServiceHost.SPEC_RETURN_VALUE_PREFIX_4th + requestValue);
    } catch (NoClientStubException | InterruptedException | IOException | MultiSocketValidException
        | ConnectionCloseException e) {
      e.printStackTrace();
    } catch (SpeculationFailException e) {
      ;
    }
    return result;
  }

  public String testMultiSpecReturnValueWithCorrectSpec(String requestValue) {
    String result = IterServiceHost.RETURN_VALUE_PREFIX + requestValue;
    try {
      this.specRPCFacade.specReturn(IterServiceHost.SPEC_RETURN_VALUE_PREFIX_1st + requestValue);
      this.specRPCFacade.specReturn(IterServiceHost.SPEC_RETURN_VALUE_PREFIX_2nd + requestValue);
      this.specRPCFacade.specReturn(result);
      this.doWork();
      this.specRPCFacade.specReturn(IterServiceHost.SPEC_RETURN_VALUE_PREFIX_3rd + requestValue);
      this.specRPCFacade.specReturn(IterServiceHost.SPEC_RETURN_VALUE_PREFIX_4th + requestValue);
    } catch (NoClientStubException | InterruptedException | IOException | MultiSocketValidException
        | ConnectionCloseException e) {
      e.printStackTrace();
    } catch (SpeculationFailException e) {
      ;
    }
    return result;
  }

  // test specBlock in server
  public String testSpecBlockReturnValue(String requestValue) {
    String result = IterServiceHost.RETURN_VALUE_PREFIX + requestValue;
    this.doWork();

    Assert.assertEquals(true, assertSpecBlock());
    return result;
  }

  public String testSpecBlockBeforeAnyReturn(String requestValue) {
    String result = IterServiceHost.RETURN_VALUE_PREFIX + requestValue;
    try {
      Assert.assertEquals(true, assertSpecBlock());
      this.doWork();
      this.specRPCFacade.specReturn(IterServiceHost.SPEC_RETURN_VALUE_PREFIX_1st + requestValue);
      this.specRPCFacade.specReturn(result);
    } catch (NoClientStubException | InterruptedException | IOException | MultiSocketValidException
        | ConnectionCloseException e) {
      e.printStackTrace();
    } catch (SpeculationFailException e) {
      e.printStackTrace();// should not happen after specBlock();
      Assert.assertEquals(true, false);
    }
    return result;
  }

  public String testSpecBlockIncorrectSpecReturnValue(String requestValue) {
    String result = IterServiceHost.RETURN_VALUE_PREFIX + requestValue;
    try {
      this.specRPCFacade.specReturn(IterServiceHost.SPEC_RETURN_VALUE_PREFIX_1st + requestValue);
      this.doWork();
      Assert.assertEquals(true, assertSpecBlock());
    } catch (NoClientStubException | InterruptedException | IOException | MultiSocketValidException
        | ConnectionCloseException e) {
      e.printStackTrace();
    } catch (SpeculationFailException e) {
      e.printStackTrace();// should not happen after specBlock();
      Assert.assertEquals(true, false);
    }
    return result;
  }

  public String testSpecBlockCorrectSpecReturnValue(String requestValue) {
    String result = IterServiceHost.RETURN_VALUE_PREFIX + requestValue;
    try {
      this.specRPCFacade.specReturn(result);
      this.doWork();
      Assert.assertEquals(true, assertSpecBlock());
    } catch (NoClientStubException | InterruptedException | IOException | MultiSocketValidException
        | ConnectionCloseException e) {
      e.printStackTrace();
    } catch (SpeculationFailException e) {
      e.printStackTrace();// should not happen after specBlock();
      Assert.assertEquals(true, false);
    }
    return result;
  }

  public String testSpecBlockMultiIncorrectSpecReturnValue(String requestValue) {
    String result = IterServiceHost.RETURN_VALUE_PREFIX + requestValue;
    try {
      this.specRPCFacade.specReturn(IterServiceHost.SPEC_RETURN_VALUE_PREFIX_1st + requestValue);
      this.specRPCFacade.specReturn(IterServiceHost.SPEC_RETURN_VALUE_PREFIX_2nd + requestValue);
      this.doWork();
      Assert.assertEquals(true, assertSpecBlock());
      this.specRPCFacade.specReturn(IterServiceHost.SPEC_RETURN_VALUE_PREFIX_3rd + requestValue);
      this.specRPCFacade.specReturn(IterServiceHost.SPEC_RETURN_VALUE_PREFIX_4th + requestValue);
    } catch (NoClientStubException | InterruptedException | IOException | MultiSocketValidException
        | ConnectionCloseException e) {
      e.printStackTrace();
    } catch (SpeculationFailException e) {
      e.printStackTrace();// should not happen after specBlock();
      Assert.assertEquals(true, false);
    }
    return result;
  }

  public String testSpecBlockMultiSpecReturnValueWithCorrectSpec(String requestValue) {
    String result = IterServiceHost.RETURN_VALUE_PREFIX + requestValue;
    try {
      this.specRPCFacade.specReturn(IterServiceHost.SPEC_RETURN_VALUE_PREFIX_1st + requestValue);
      this.specRPCFacade.specReturn(IterServiceHost.SPEC_RETURN_VALUE_PREFIX_2nd + requestValue);
      this.specRPCFacade.specReturn(result);
      this.doWork();
      Assert.assertEquals(true, assertSpecBlock());
      this.specRPCFacade.specReturn(IterServiceHost.SPEC_RETURN_VALUE_PREFIX_3rd + requestValue);
      this.specRPCFacade.specReturn(IterServiceHost.SPEC_RETURN_VALUE_PREFIX_4th + requestValue);
    } catch (NoClientStubException | InterruptedException | IOException | MultiSocketValidException
        | ConnectionCloseException e) {
      e.printStackTrace();
    } catch (SpeculationFailException e) {
      e.printStackTrace();// should not happen after specBlock();
      Assert.assertEquals(true, false);
    }
    return result;
  }

  private boolean assertSpecBlock() {
    try {
      this.specRPCFacade.specBlock();

      Assert.assertEquals(SpeculationStatus.SUCCEED, this.specRPCFacade.getCalleeStatus());
      Assert.assertEquals(SpeculationStatus.SUCCEED, this.specRPCFacade.getCallerStatus());
      Assert.assertEquals(SpeculationStatus.SUCCEED, this.specRPCFacade.getCurrentRpcStatus());
      return true;
    } catch (SpeculationFailException e) {
      Assert.assertEquals(SpeculationStatus.FAIL, this.specRPCFacade.getCurrentRpcStatus());
      return true;
    } catch (InterruptedException e) {
      e.printStackTrace();
      return false;
    }
  }

  // test exception return
  public String testReturnException(String requestValue) {
    try {
      this.specRPCFacade.throwNonSpecExceptionToClient(IterServiceHost.RETURN_EXCEPTION_PREFIX + requestValue);
    } catch (NoClientStubException | InterruptedException | IOException | MultiSocketValidException
        | ConnectionCloseException e) {
      e.printStackTrace();
    } catch (SpeculationFailException e) {
      ;
    }

    return null;
  }

  public String testReturnExceptionAfterSpecReturn(String requestValue) {
    try {
      this.specRPCFacade.specReturn(IterServiceHost.SPEC_RETURN_VALUE_PREFIX_1st + requestValue);
      this.doWork();
      this.specRPCFacade.throwNonSpecExceptionToClient(IterServiceHost.RETURN_EXCEPTION_PREFIX + requestValue);
    } catch (NoClientStubException | InterruptedException | IOException | MultiSocketValidException
        | ConnectionCloseException e) {
      e.printStackTrace();
    } catch (SpeculationFailException e) {
      ;
    }

    return null;
  }
}
