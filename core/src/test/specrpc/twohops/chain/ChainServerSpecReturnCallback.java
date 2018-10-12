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

import Waterloo.MultiSocket.exception.ConnectionCloseException;
import Waterloo.MultiSocket.exception.MultiSocketValidException;
import rpc.execption.NoClientStubException;
import specrpc.exception.SpeculationFailException;

public class ChainServerSpecReturnCallback extends ChainServerReturnCallback {

  public ChainServerSpecReturnCallback() {

  }

  @Override
  public Object run(Object rpcReturnValue) throws SpeculationFailException, InterruptedException {

    String result = ChainServiceMessages.MID_SERVER_RESPONSE_PREFIX + rpcReturnValue.toString();

    try {
      specRPCFacade
          .specReturn(ChainServiceMessages.MID_SERVER_SPEC_RESPONSE_PREFIX_1 + rpcReturnValue.toString());
      specRPCFacade.specReturn(result);
    } catch (NoClientStubException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    } catch (MultiSocketValidException e) {
      e.printStackTrace();
    } catch (ConnectionCloseException e) {
      e.printStackTrace();
    }

    return result;
  }

}
