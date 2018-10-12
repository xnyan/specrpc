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

package micro.client;

import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import micro.client.request.MicroRequest;
import micro.common.MicroConstants;

public abstract class MicroClient {

  private static final Logger logger = LoggerFactory.getLogger(MicroConstants.LOGGER_TYPE);
  
  // Client instance
  protected final String clientId;
  protected final String rpcConfigFile;

  public MicroClient(Properties clientConfig) {
    this.clientId = clientConfig.getProperty(MicroConstants.CLIENT_ID_PROPERTY);
    this.rpcConfigFile = clientConfig.getProperty(MicroConstants.RPC_CONFIG_FILE_PROPERTY);
    this.initRpcFramework(clientConfig);
  }

  public String getClientId() {
    return this.clientId;
  }

  protected void doLocalComputation(long compTime) {
    try {
      if (compTime > 0) {
        Thread.sleep(compTime);
      }
    } catch (InterruptedException e) {
      e.printStackTrace();
      logger.error(e.getMessage());
      System.exit(MicroConstants.RUNTIME_FATAL_ERROR_CODE);
    }
  }
  
  public abstract void initRpcFramework(Properties config);
  
  public abstract String execRequest(MicroRequest request);

  public abstract void shutdown();
}
