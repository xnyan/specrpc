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

package tradrpc.server.api;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ClosedSelectorException;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import Waterloo.MultiSocket.MultiSocketServer;

import rpc.config.Constants;
import rpc.execption.UninitializationException;
import rpc.server.api.RpcHostObjectFactory;
import rpc.server.api.RpcServer;
import specrpc.common.RpcConfig;
import specrpc.common.Location;
import specrpc.common.RpcSignature;
import specrpc.common.ServerLocationDirectory;
import tradrpc.server.TradRpcConnectionHandler;
import tradrpc.server.TradRpcHostObjectMap;

public class TradRpcServer implements RpcServer {

  private boolean initialized = false;
  private boolean terminating = true;

  private ServerLocationDirectory directory;
  private MultiSocketServer serverSocket;

  private ExecutorService serverThreadPool;
  private Location serverLocation;
  private String serverIdentity;
  private TradRpcHostObjectMap localdir;

  public synchronized void initServer(String id, String ip, int port, int threadPoolSize, int maxConnectionNum,
      String rpcSigaturesFile) throws FileNotFoundException, IOException {
    if (initialized) {
      return;
    }
    directory = new ServerLocationDirectory(rpcSigaturesFile);
    localdir = new TradRpcHostObjectMap();
    serverIdentity = id;
    serverThreadPool = threadPoolSize > 0 ? Executors.newFixedThreadPool(threadPoolSize)
        : Executors.newCachedThreadPool();
    serverSocket = new MultiSocketServer(new TradRpcConnectionHandler(serverThreadPool, localdir),
        new InetSocketAddress(ip, port), maxConnectionNum);
    serverLocation = new Location(ip, serverSocket.getLocalPort());

    initialized = true;
  }

  public synchronized void initServer(String configFile) throws IOException {
    RpcConfig config = new RpcConfig(configFile);
    initServer(config.get(Constants.RPC_HOST_ID_PROPERTY, Constants.DEFAULT_RPC_HOST_ID), configFile);
  }

  public synchronized void initServer(String id, String configFile) throws FileNotFoundException, IOException {
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
      throw new UninitializationException("You should call initServer() before calling register().");
    }

    RpcSignature signature = new RpcSignature(hostClassFactory.getRpcHostClassName(), methodName, returnType,
        argTypes);

    localdir.register(signature, hostClassFactory);
    return signature;
  }

  public synchronized void register(String methodName, RpcHostObjectFactory hostClassFactory, Class<?> returnType,
      Class<?>... argTypes) throws UninitializationException {
    RpcSignature signature = this.doRegister(methodName, hostClassFactory, returnType, argTypes);
    this.directory.registerAndPersist(serverIdentity, signature, serverLocation);
  }

  public synchronized void registerNotPersist(String methodName, RpcHostObjectFactory hostClassFactory,
      Class<?> returnType, Class<?>... argTypes) throws UninitializationException {
    RpcSignature signature = this.doRegister(methodName, hostClassFactory, returnType, argTypes);
    this.directory.registerNotPersist(serverIdentity, signature, serverLocation);
  }

  public synchronized void persistRpcSig() {
    this.directory.persistRpcSig();
  }

  public synchronized HashMap<String, Location> getRpcSigLocationTable() {
    return this.directory.getRpcSigLocation();
  }

  public void execute() throws UninitializationException, InterruptedException, IOException {
    if (!initialized) {
      throw new UninitializationException("ComServer: You should call initServer() before calling execute().");
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

    // Terminates thread pool
    if (!serverThreadPool.awaitTermination(60, TimeUnit.SECONDS)) {
      // Cancels currently executing tasks
      serverThreadPool.shutdownNow();
      // Waits a while for tasks to respond to being cancelled
      if (!serverThreadPool.awaitTermination(60, TimeUnit.SECONDS))
        // TODO: Uses a better solution
        System.err.println("Thread pool did not terminate properly.");
    }
  }

  // This is used to locally terminate server from a different thread
  public synchronized void terminate() throws IOException {
    if (terminating == true && initialized == false) {
      return;
    }
    terminating = true;
    initialized = false;
    serverSocket.close();
  }
}
