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
import java.util.ArrayList;

import junit.framework.Assert;

import rpc.execption.MethodNotRegisteredException;
import rpc.execption.UserException;
import specrpc.client.api.SpecRpcFuture;
import specrpc.client.api.SpecRpcClientStub;
import specrpc.common.RpcSignature;
import specrpc.exception.SpeculationFailException;

public class IterClientExceptionCallback extends IterClientCallback {

  public IterClientExceptionCallback(int index, RpcSignature methodSignature, String requestValue,
      ArrayList<Object> predictedValues) {
    super(index, methodSignature, requestValue, predictedValues);
  }

  @Override
  public Object run(Object rpcReturnValue) throws SpeculationFailException, InterruptedException {
    try {
      String result = rpcReturnValue.toString();
      if (this.indexInIterativePattern > 1) {// iterative calls
        SpecRpcClientStub srvStub = specRPCFacade.bind(IterTest.SERVER_IDENTITY, this.methodSignature);
        SpecRpcFuture future = srvStub.call(this.predictedValues,
            new IterClientExceptionCallbackFactory(this.indexInIterativePattern - 1, this.methodSignature,
                this.requestValue, this.predictedValues),
            this.requestValue);
        result = result + future.getResult().toString();
      }
      return IterClient.CALLBACK_RESULT_PREFIX + this.indexInIterativePattern + " " + result;
    } catch (MethodNotRegisteredException | IOException e) {
      e.printStackTrace();
    } catch (UserException e) {
      try {
        specRPCFacade.specBlock();
      } catch (SpeculationFailException e1) {
        return e.getMessage();
      }
      Assert.assertEquals(IterClient.EXPECTED_EXCEPTION, e.getMessage());
      return e.getMessage();
    }

    return null;
  }
}
