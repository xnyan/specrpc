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

package micro.benchmark;

import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import micro.common.MicroConstants;
import micro.server.MicroServer;

public class Server {

  private static final Logger logger = LoggerFactory
      .getLogger(MicroConstants.LOGGER_TYPE);

  public static void main(String args[]) {
    Properties config = ServerArgs.parseArgs(args);
    String id = config.getProperty(MicroConstants.SERVER_ID_PROPERTY);
    String ip = config.getProperty(MicroConstants.SERVER_IP_PROPERTY);
    int port = Integer
        .parseInt(config.getProperty(MicroConstants.SERVER_PORT_PROPERTY));

    MicroServer server = new MicroServer(id, ip, port, config);
    Thread serverThread = new Thread(server);
    serverThread.start();

    logger.debug("MicroServer starts id = " + server.serverId + ", ip = "
        + server.ip + ", port = " + server.port);
  }
}
