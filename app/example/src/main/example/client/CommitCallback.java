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

import rpc.config.Constants;
import specrpc.client.api.SpecRpcCallback;
import specrpc.common.api.SpecRpcFacade;
import specrpc.exception.SpeculationFailException;

public class CommitCallback implements SpecRpcCallback {

  private static final Logger logger = LoggerFactory.getLogger(Constants.LOGGER_TYPE);

  private SpecRpcFacade specRPCFacade;

  @Override
  public void bind(SpecRpcFacade specRPCFacade) {
    this.specRPCFacade = specRPCFacade;
  }

  @Override
  public Object run(Object rpcReturnValue) throws SpeculationFailException, InterruptedException {
    logger.info("Callback has rpc return value = " + rpcReturnValue.toString() + ", callback status = "
        + this.specRPCFacade.getCurrentRpcStatus());
    return rpcReturnValue;
  }

}
