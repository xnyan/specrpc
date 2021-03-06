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

import rpc.execption.MethodNotRegisteredException;
import rpc.execption.UserException;
import specrpc.client.api.SpecRpcCallbackObject;
import specrpc.client.api.SpecRpcFuture;
import specrpc.client.api.SpecRpcClientStub;
import specrpc.common.RpcSignature;
import specrpc.exception.SpeculationFailException;

public class IterClientCallback extends SpecRpcCallbackObject {

  protected final int indexInIterativePattern;
  protected final RpcSignature methodSignature;
  protected final String requestValue;
  protected final ArrayList<Object> predictedValues;

  public IterClientCallback(int index, RpcSignature methodSignature, String requestValue,
      ArrayList<Object> predictedValues) {
    this.indexInIterativePattern = index;
    this.methodSignature = methodSignature;
    this.requestValue = requestValue;
    this.predictedValues = predictedValues;// may be null
  }

  @Override
  public Object run(Object rpcReturnValue) throws SpeculationFailException, InterruptedException {
    try {
      String result = rpcReturnValue.toString();
      if (this.indexInIterativePattern > 1) {// iterative calls
        SpecRpcClientStub srvStub = specRPCFacade.bind(IterTest.SERVER_IDENTITY, this.methodSignature);
        SpecRpcFuture future = srvStub.call(this.predictedValues,
            new IterClientCallbackFactory(this.indexInIterativePattern - 1, this.methodSignature, this.requestValue,
                this.predictedValues),
            this.requestValue);
        result = result + future.getResult().toString();
      }
      return IterClient.CALLBACK_RESULT_PREFIX + this.indexInIterativePattern + " " + result;
    } catch (MethodNotRegisteredException | IOException | UserException e) {
      e.printStackTrace();
    }

    return null;
  }

}
