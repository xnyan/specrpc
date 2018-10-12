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

package specrpc.server;

import java.io.IOException;

import Waterloo.MultiSocket.exception.ConnectionCloseException;
import Waterloo.MultiSocket.exception.MultiSocketValidException;
import rpc.execption.NoClientStubException;
import specrpc.common.Status.SpeculationStatus;
import specrpc.exception.SpeculationFailException;

public interface SpecRpcServerStub {

  void sendNonSpecReturn(Object value) throws NoClientStubException, SpeculationFailException, InterruptedException,
      IOException, MultiSocketValidException, ConnectionCloseException;

  void sendSpecReturn(Object value) throws NoClientStubException, SpeculationFailException, InterruptedException,
      IOException, MultiSocketValidException, ConnectionCloseException;

  void throwNonSpecException(String message) throws NoClientStubException, SpeculationFailException,
      InterruptedException, IOException, MultiSocketValidException, ConnectionCloseException;

  // For ClientStub implementation in Callback
  void callbackStatusChanged(SpeculationStatus callbackStatus);
}
