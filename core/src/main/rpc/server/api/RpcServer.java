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

package rpc.server.api;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;

import rpc.execption.UninitializationException;
import specrpc.common.Location;

public interface RpcServer {

  public void initServer(String configFile) throws IOException;

  public void initServer(String id, String configFile) throws IOException; // Legacy interface

  public void initServer(String id, String ip, int port, String configFile) throws IOException;

  public void initServer(String id, String ip, int port, int threadPoolSize, int maxConnectionNum,
      String rpcSigaturesFile) throws FileNotFoundException, IOException;

  /**
   * Registers an RPC and persists the RPC signature to the specified file in configuration.
   * 
   * @param methodName
   * @param hostClassFactory
   * @param returnType
   * @param argTypes
   * @throws UninitializationException
   */
  public void register(String methodName, RpcHostObjectFactory hostClassFactory, Class<?> returnType,
      Class<?>... argTypes) throws UninitializationException;
  
  /**
   * Registers an RPC but does not persist the RPC signature to the specified file in configuration.
   * 
   * @param methodName
   * @param hostClassFactory
   * @param returnType
   * @param argTypes
   * @throws UninitializationException
   */
  public void registerNotPersist(String methodName, RpcHostObjectFactory hostClassFactory, Class<?> returnType,
      Class<?>... argTypes) throws UninitializationException;
  
  /**
   * Persists all registered RPC signatures to the specified file in configuration.
   */
  public void persistRpcSig();
  
  public HashMap<String, Location> getRpcSigLocationTable();

  /**
   * Starts running RPC server.
   * 
   * @throws UninitializationException
   * @throws InterruptedException
   * @throws IOException
   */
  public void execute() throws UninitializationException, InterruptedException, IOException;

  /**
   * Shuts down the running RPC server.
   * 
   * @throws IOException
   */
  public void terminate() throws IOException;
}
