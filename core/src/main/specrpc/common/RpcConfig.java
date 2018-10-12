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

package specrpc.common;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rpc.config.Constants;

public class RpcConfig {

  private static final Logger logger = LoggerFactory.getLogger(Constants.LOGGER_TYPE);

  private final Properties prop;

  public RpcConfig(String configFile) throws FileNotFoundException, IOException {
    if (configFile == null || configFile.isEmpty()) {
      logger.warn("Miss RPC framework configuration file. Use default one: " + Constants.DEFAULT_RPC_CONFIG_FILE);
      Constants.getSpecRpcHome();
      configFile = Constants.DEFAULT_RPC_CONFIG_FILE;
    }
    prop = new Properties();
    FileInputStream input = new FileInputStream(configFile);
    prop.load(input);
    input.close();
  }

  public String get(String key, String defaultValue) {
    return prop.getProperty(key, defaultValue);
  }

  public String get(String key) {
    return prop.getProperty(key);
  }
}
