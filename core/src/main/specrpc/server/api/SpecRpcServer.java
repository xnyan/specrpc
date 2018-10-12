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

package specrpc.server.api;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ClosedSelectorException;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import Waterloo.MultiSocket.MultiSocketServer;

import rpc.config.Constants;
import rpc.execption.UninitializationException;
import rpc.server.api.RpcHostObjectFactory;
import rpc.server.api.RpcServer;
import specrpc.common.RpcConfig;
import specrpc.common.Location;
import specrpc.common.RpcSignature;
import specrpc.common.ServerLocationDirectory;
import specrpc.server.SpecRpcConnectionHandler;
import specrpc.server.SpecRpcHostObjectMap;

public class SpecRpcServer implements RpcServer {

  protected static final Logger logger = LoggerFactory.getLogger(Constants.LOGGER_TYPE);

  private boolean initialized = false;
  private boolean terminating = true;

  // Provides threads for receiving RPC request messages and executing RPC methods
  private ExecutorService serverThreadPool;
  // private ServerSocket serverSocket;
  private MultiSocketServer serverSocket;
  private String serverIdentity;
  private ServerLocationDirectory serverLocationDir;
  private Location serverLocation;
  private SpecRpcHostObjectMap hostObjectDir;

  public synchronized void initServer(String id, String ip, int port, int threadPoolSize, int maxConnectionNum,
      String rpcSigaturesFile) throws FileNotFoundException, IOException {
    if (initialized) {
      return;
    }
    serverLocationDir = new ServerLocationDirectory(rpcSigaturesFile);
    hostObjectDir = new SpecRpcHostObjectMap();
    serverIdentity = id;
    // Uses dynamic thread pool for testing
    serverThreadPool = threadPoolSize > 0 ? Executors.newFixedThreadPool(threadPoolSize)
        : Executors.newCachedThreadPool();
    serverSocket = new MultiSocketServer(new SpecRpcConnectionHandler(serverThreadPool, hostObjectDir),
        new InetSocketAddress(ip, port), maxConnectionNum);
    /*
     * // Java Server Socket Implementation
     * 
     * serverSocket = new ServerSocket();//0, maxConnectionNum);
     * 
     * serverSocket.setReuseAddress(true);
     * 
     * serverSocket.bind(new InetSocketAddress(ip, port), maxConnectionNum);
     */
    serverLocation = new Location(ip, serverSocket.getLocalPort());

    initialized = true;
  }

  public synchronized void initServer(String configFile) throws IOException {
    RpcConfig config = new RpcConfig(configFile);
    initServer(config.get(Constants.RPC_HOST_ID_PROPERTY, Constants.DEFAULT_RPC_HOST_ID), configFile);
  }

  // Legacy interface
  public synchronized void initServer(String id, String configFile) throws IOException {
    RpcConfig config = new RpcConfig(configFile);
    String ip = config.get(Constants.RPC_HOST_IP_PROPERTY, Constants.DEFAULT_RPC_HOST_IP);
    int port = Integer.parseInt(config.get(Constants.RPC_HOST_PORT_PROPERTY, Constants.DEFAULT_RPC_HOST_PORT));
    int threadPoolSize = Integer
        .parseInt(config.get(Constants.RPC_HOST_THREADPOOL_SIZE_PROPERTY, Constants.DEFAULT_RPC_HOST_THREADPOOL_SIZE));
    int maxConnectionNum = Integer
        .parseInt(config.get(Constants.RPC_HOST_MAX_CONNECTION_PROPERTY, Constants.DEFAULT_RPC_HOST_MAX_CONNECTION));
    String rpcSigaturesFile = config.get(Constants.RPC_HOST_SIGNATURE_FILE_PROPERTY,
        Constants.DEFAULT_RPC_HOST_SIGNATURE_FILE);
    initServer(id, ip, port, threadPoolSize, maxConnectionNum, rpcSigaturesFile);
  }

  public synchronized void initServer(String id, String ip, int port, String configFile) throws IOException {
    RpcConfig config = new RpcConfig(configFile);
    int threadPoolSize = Integer
        .parseInt(config.get(Constants.RPC_HOST_THREADPOOL_SIZE_PROPERTY, Constants.DEFAULT_RPC_HOST_THREADPOOL_SIZE));
    int maxConnectionNum = Integer
        .parseInt(config.get(Constants.RPC_HOST_MAX_CONNECTION_PROPERTY, Constants.DEFAULT_RPC_HOST_MAX_CONNECTION));
    String rpcSigaturesFile = config.get(Constants.RPC_HOST_SIGNATURE_FILE_PROPERTY,
        Constants.DEFAULT_RPC_HOST_SIGNATURE_FILE);
    initServer(id, ip, port, threadPoolSize, maxConnectionNum, rpcSigaturesFile);
  }

  private RpcSignature doRegister(String methodName, RpcHostObjectFactory hostClassFactory, Class<?> returnType,
      Class<?>... argTypes) throws UninitializationException {
    if (!initialized) {
      throw new UninitializationException("Calls initServer() before calling register().");
    }

    RpcSignature signature = new RpcSignature(hostClassFactory.getRpcHostClassName(), methodName, returnType,
        argTypes);

    hostObjectDir.register(signature, hostClassFactory);
    return signature;
  }

  public synchronized void register(String methodName, RpcHostObjectFactory hostClassFactory, Class<?> returnType,
      Class<?>... argTypes) throws UninitializationException {
    RpcSignature signature = this.doRegister(methodName, hostClassFactory, returnType, argTypes);
    serverLocationDir.registerAndPersist(serverIdentity, signature, serverLocation);
  }

  /**
   * Register an RPC signature but not persist it into file
   * 
   * @param methodName
   * @param hostClassFactory
   * @param returnType
   * @param argTypes
   * @throws UninitializationException
   */
  public synchronized void registerNotPersist(String methodName, RpcHostObjectFactory hostClassFactory,
      Class<?> returnType, Class<?>... argTypes) throws UninitializationException {
    RpcSignature signature = this.doRegister(methodName, hostClassFactory, returnType, argTypes);
    this.serverLocationDir.registerNotPersist(serverIdentity, signature, serverLocation);
  }

  public synchronized void persistRpcSig() {
    this.serverLocationDir.persistRpcSig();
  }

  public synchronized HashMap<String, Location> getRpcSigLocationTable() {
    return this.serverLocationDir.getRpcSigLocation();
  }

  public void execute() throws UninitializationException, InterruptedException, IOException {
    if (!initialized) {
      throw new UninitializationException("You should call initServer() before calling execute().");
    }

    terminating = false;
    while (!terminating) {
      try {
        serverSocket.accept();
      } catch (IOException e) {
        if (terminating)
          break;
        else
          throw e;
      } catch (ClosedSelectorException e) {
        if (terminating)
          break;
        else
          throw e;
      }
    }

    serverThreadPool.shutdown();
    serverSocket.close();

    // Terminates the thread pool
    if (!serverThreadPool.awaitTermination(60, TimeUnit.SECONDS)) {
      serverThreadPool.shutdownNow(); // Cancels currently executing tasks
      // Waits for tasks to be cancelled
      if (!serverThreadPool.awaitTermination(60, TimeUnit.SECONDS))
        // TODO: Uses a better solution
        logger.error("Thread poll does not terminate properly");
    }
  }

  // Terminates server
  public synchronized void terminate() throws IOException {
    if (terminating == true && initialized == false) {
      return;
    }
    terminating = true;
    initialized = false;
    serverSocket.close();
  }
}
