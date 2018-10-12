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

package rc.server.tradrpc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rc.common.RcConstants;
import rc.server.RcService;
import tradrpc.server.TradRpcClientStub;
import tradrpc.server.api.TradRpcHost;

public class RcServiceTradRpc extends RcService implements TradRpcHost {

  private static final Logger logger = LoggerFactory.getLogger(RcConstants.LOGGER_TYPE);
  
  private TradRpcClientStub tradRpc;

  public RcServiceTradRpc() {

  }

  @Override
  public void bind(TradRpcClientStub clientStub) {
    this.tradRpc = clientStub;
  }

  @Override
  public Boolean prepareTxnAfterSpec(String txnId, String[] readKeyList, String[] writeKeyList, String[] writeValList,
      String[] nonRequiredReadKeyList) {
    logger.error("Should not call prepareTxnAfterSpec() with TradRPC framework. Debug!");
    System.exit(RcConstants.RUNTIME_FATAL_ERROR_CODE);
    return null;
  }

}
