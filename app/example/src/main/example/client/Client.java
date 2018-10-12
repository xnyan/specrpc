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

package example.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import example.server.CommitResult;
import rpc.config.Constants;

public class Client {

  private static final Logger logger = LoggerFactory.getLogger(Constants.LOGGER_TYPE);

  public static void main(String args[]) {
    ClientLib lib = new ClientLib(null);
    String[] readKeyList = {};
    String[] writeKeyList = { "A", "B", "C" };
    String[] writeValList = { "a1", "b2", "c3" };
    CommitResult result = lib.commit("1-1", readKeyList, writeKeyList, writeValList);
    logger.info("Client received: " + result.toString());
    lib.close();
  }
}
