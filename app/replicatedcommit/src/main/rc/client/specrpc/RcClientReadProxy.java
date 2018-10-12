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

package rc.client.specrpc;

import java.io.IOException;
import java.util.HashMap;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rc.common.RcConstants;
import rpc.execption.UninitializationException;
import rpc.server.api.RpcServer;
import specrpc.common.Location;
import specrpc.common.RpcSignature;
import specrpc.server.api.SpecRpcServer;

/**
 * A proxy server to facilitate speculation on a Quorum read.
 */
public class RcClientReadProxy implements Runnable {
  
  private static final Logger logger = LoggerFactory.getLogger(RcConstants.LOGGER_TYPE);
  
  // TODO Extends SpecRPC to support local non-RPC calls.
  // This is a local proxy to the client.
  private final String ip = "localhost";
  private final String clientId;
  private final int port;
  private final RpcServer rpcServer;
  private boolean isRpcServerStarted = false;
  
  public RcClientReadProxy(String clientId, int port, Properties config) {
    this.clientId = clientId;
    this.port = port;
    String rpcConfigFile = config.getProperty(RcConstants.RPC_CONFIG_FILE_PROPERTY);
    // Initializes RPC framework
    this.rpcServer = new SpecRpcServer();
    RcClientReadProxyServiceFactory rpcHostObjectFactory = new RcClientReadProxyServiceFactory();
    try {
      this.rpcServer.initServer(clientId, this.ip, port,rpcConfigFile);
      
      // Registers RPC signatures.
      RpcSignature[] rpcSigList = {
          RcClientReadProxyService.RPC_QUORUM_READ
      };
      for (RpcSignature rpcSig : rpcSigList) {
        //this.rpcServer.register(rpcSig.methodName, rpcHostObjectFactory, rpcSig.returnType, rpcSig.argTypes);
        this.rpcServer.registerNotPersist(rpcSig.methodName, rpcHostObjectFactory, rpcSig.returnType, rpcSig.argTypes);
      }
    } catch (IOException | UninitializationException e) {
      logger.error(e.getMessage());
      System.exit(RcConstants.INIT_FAIL_ERROR_CODE);
    }
  }

  @Override
  public void run() {
    // Starts RPC framework
    try {
      logger.debug("Client id = " + this.clientId +
          " is starting a read proxy with SpecRPC at ip=" + this.ip +
          ", port=" + port);
      this.markRpcServerStarted();
      this.rpcServer.execute();
    } catch (UninitializationException | InterruptedException | IOException e) {
      logger.error(e.getMessage());
      System.exit(RcConstants.INIT_FAIL_ERROR_CODE);
    }
  }
  
  public synchronized HashMap<String, Location> getRpcSigLocationTable() {
    return this.rpcServer.getRpcSigLocationTable();
  }
  
  private synchronized void markRpcServerStarted() {
    this.isRpcServerStarted = true;
    this.notifyAll();
  }
  
  public synchronized void waitRpcServerToStart() {
    while (this.isRpcServerStarted == false) {
      try {
        this.wait();
      } catch (InterruptedException e) {
        logger.error(e.getMessage());
        System.exit(RcConstants.INIT_FAIL_ERROR_CODE);
      }
    }
  }
  
  public void shutdown() throws IOException {
    this.rpcServer.terminate();
  }

}
