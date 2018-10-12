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
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicInteger;

import Waterloo.MultiSocket.exception.ConnectionCloseException;
import Waterloo.MultiSocket.exception.MultiSocketValidException;

import rpc.execption.NoClientStubException;
import specrpc.exception.SpeculationFailException;

public class ChainServerSpecBlockCallback extends ChainClientSpecBlockCallback {

  public ChainServerSpecBlockCallback(CyclicBarrier allCallbacksBarrier, AtomicInteger passSpecBlockNum,
      AtomicInteger failSpecBlockNum) {
    super(allCallbacksBarrier, passSpecBlockNum, failSpecBlockNum);
  }

  @Override
  public Object run(Object rpcReturnValue) throws SpeculationFailException, InterruptedException {

    try {
      specRPCFacade.specReturn(ChainServiceMessages.MID_SERVER_SPEC_RESPONSE_PREFIX_1 + rpcReturnValue);
    } catch (NoClientStubException e) {
      e.printStackTrace();
    } catch (SpeculationFailException e) {
      try {
        this.failSpecBlockNum.incrementAndGet();
        this.allCallbacksBarrier.await();
        throw e;
      } catch (BrokenBarrierException e1) {
        e1.printStackTrace();
      }
    } catch (IOException e) {
      e.printStackTrace();
    } catch (MultiSocketValidException e) {
      e.printStackTrace();
    } catch (ConnectionCloseException e) {
      e.printStackTrace();
    }

    try {
      specRPCFacade.specBlock();
      specRPCFacade.specReturn(ChainServiceMessages.MID_SERVER_RESPONSE_PREFIX + rpcReturnValue.toString());
      this.passSpecBlockNum.incrementAndGet();
      this.allCallbacksBarrier.await();
    } catch (BrokenBarrierException e) {
      e.printStackTrace();
    } catch (SpeculationFailException e) {
      try {
        this.failSpecBlockNum.incrementAndGet();
        this.allCallbacksBarrier.await();
        throw e;
      } catch (BrokenBarrierException e1) {
        e1.printStackTrace();
      }
    } catch (NoClientStubException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    } catch (MultiSocketValidException e) {
      e.printStackTrace();
    } catch (ConnectionCloseException e) {
      e.printStackTrace();
    }

    return ChainServiceMessages.MID_SERVER_RESPONSE_PREFIX + rpcReturnValue.toString();
  }

}
