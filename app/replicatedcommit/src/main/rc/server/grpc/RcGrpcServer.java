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

package rc.server.grpc;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import rc.common.RcConstants;

public class RcGrpcServer {
  
  private static final Logger logger = LoggerFactory.getLogger(RcConstants.LOGGER_TYPE);

  private final Server grpcServer;

  public RcGrpcServer(int port) {
    this.grpcServer = ServerBuilder.forPort(port).addService(new RcServiceGrpcImpl()).build();
  }
  
  public void execute() throws IOException {
    this.grpcServer.start();
    try {
      this.grpcServer.awaitTermination();
    } catch (InterruptedException e) {
      e.printStackTrace();
      logger.error(e.getMessage());
      System.exit(RcConstants.RUNTIME_FATAL_ERROR_CODE);
    }
  }
}
