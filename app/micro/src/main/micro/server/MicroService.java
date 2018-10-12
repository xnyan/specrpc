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

package micro.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import micro.common.MicroConstants;

import specrpc.common.RpcSignature;

public abstract class MicroService {

  private static final Logger logger = LoggerFactory.getLogger(MicroConstants.LOGGER_TYPE);

  // RPCs
  
  public static final String RPC_ONE_HOP_NAME = "oneHop";
  public static final RpcSignature RPC_ONE_HOP = new RpcSignature(
      MicroService.class.getName(), // class name
      RPC_ONE_HOP_NAME, // method name
      String.class, // return type
      String.class // input data
  );
  /**
   * Simulates the computation of an RPC
   * 
   * @param data,
   *          input data for computation
   * @return data
   */
  public String oneHop(String data) {
    this.doComputation(MicroServer.getComputationTimeInTotal());
    return data;
  }
  
  public static final String RPC_MULTI_HOP_NAME = "multiHop";
  public static final RpcSignature RPC_MULTI_HOP = new RpcSignature(
      MicroService.class.getName(), // class name
      RPC_MULTI_HOP_NAME, // method name
      String.class, // return type
      String.class, // input data
      Integer.class // number of hops left
  );
  /**
   * Simulates the computation of an RPC that requires performing another RPC to another server
   * 
   * @param data
   * @param hopNum
   * @return data
   */
  public abstract String multiHop(String data, Integer hopNum);
  
  protected void doComputation(long time) {
    if (time > 0) {
      try {
        Thread.sleep(time);
      } catch (InterruptedException e) {
        e.printStackTrace();
        logger.error(e.getMessage());
        System.exit(MicroConstants.RUNTIME_FATAL_ERROR_CODE);
      }
    }
  }
      
}
