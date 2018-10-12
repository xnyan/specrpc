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
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicInteger;

import junit.framework.Assert;

import rpc.execption.MethodNotRegisteredException;
import rpc.execption.UserException;
import specrpc.client.api.SpecRpcFuture;
import specrpc.client.api.SpecRpcClientStub;
import specrpc.common.RpcSignature;
import specrpc.exception.SpeculationFailException;

public class IterClientPostSpecBlockCallback extends IterClientSpecBlockCallback {

  public IterClientPostSpecBlockCallback(int index, RpcSignature methodSignature, String requestValue,
      ArrayList<Object> predictedValues, CyclicBarrier allCallbacksBarrier, AtomicInteger passSpecBlockNum,
      AtomicInteger failSpecBlockNum) {
    super(index, methodSignature, requestValue, predictedValues, allCallbacksBarrier, passSpecBlockNum,
        failSpecBlockNum);
  }

  @Override
  public Object run(Object rpcReturnValue) throws SpeculationFailException, InterruptedException {
    try {
      String result = rpcReturnValue.toString();

      specRPCFacade.specBlock();
      Assert.assertEquals(IterClient.CORRECT_SPEC_VALUE, rpcReturnValue.toString());
      this.passSpecBlockNum.incrementAndGet();

      if (this.indexInIterativePattern > 1) {// iterative calls
        CyclicBarrier nextLevelAllCallbacksBarrier = new CyclicBarrier(this.allCallbacksBarrier.getParties());
        SpecRpcClientStub srvStub = specRPCFacade.bind(IterTest.SERVER_IDENTITY, this.methodSignature);
        SpecRpcFuture future = srvStub.call(this.predictedValues,
            new IterClientSpecBlockCallbackFactory(this.indexInIterativePattern - 1, this.methodSignature,
                this.requestValue, this.predictedValues, nextLevelAllCallbacksBarrier, this.passSpecBlockNum,
                this.failSpecBlockNum),
            this.requestValue);
        result = result + future.getResult().toString();
      }

      return IterClient.CALLBACK_RESULT_PREFIX + this.indexInIterativePattern + " " + result;
    } catch (MethodNotRegisteredException | IOException | UserException e) {
      e.printStackTrace();
    } catch (SpeculationFailException e) {
      this.failSpecBlockNum.incrementAndGet();
      throw e;
    }

    Assert.assertEquals(true, false);// should not arrive here
    return null;
  }
}
