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

package specrpc.client.api;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import rpc.communication.Communication;
import rpc.config.Constants;
import rpc.execption.MethodNotRegisteredException;
import specrpc.client.SpecRpcServerStubObject;
import specrpc.common.RpcConfig;
import specrpc.common.Location;
import specrpc.common.RpcSignature;
import specrpc.common.ServerLocationDirectory;
import specrpc.common.Status;
import specrpc.common.Status.SpeculationStatus;

public class SpecRpcClient {
  private static ServerLocationDirectory directory;
  public static ExecutorService clientThreadPool;

  private static boolean terminated = true;

  /*
   * Interface for Caller (Client) to load the configuration file; to build the
   * directory for the mapping from RPC signature to its server location
   */
  public static synchronized void initClient(String configFile) throws IOException {
    if (terminated == false) { // has been initialized by other thread
      return;
    }

    // Initializes communication module
    Communication.initClientCommunication();

    RpcConfig config = new RpcConfig(configFile);
    // Reads from the client properties file to determine the location
    // of the directory server (host:port)
    // NOTE: This might currently not be used.
    // Location dirLocation = new Location(config.getDirHostName(),
    // config.getDirectoryPort());

    // In theory, we should be using the directory location information to
    // fetch the current directory from the directory server. However, we
    // currently just load the directory from a file on disk (currently hardcoded
    // as directory.properties 10/23/2013)
    // directory = new ServerLocationDirectory(dirLocation);
    directory = new ServerLocationDirectory(
        config.get(Constants.RPC_HOST_SIGNATURE_FILE_PROPERTY, Constants.DEFAULT_RPC_HOST_SIGNATURE_FILE));

    // Initializes the client-side thread pool
    // TODO: make sure thread pool size does not affect the performance or cause
    // deadlock
    int threadPoolSize = Integer.parseInt(config.get(Constants.SPECRPC_CLIENT_THREADPOOL_SIZE_PROPERTY,
        Constants.DEFAULT_SPECRPC_CLIENT_THREADPOOL_SIZE));
    clientThreadPool = threadPoolSize > 0 ? Executors.newFixedThreadPool(threadPoolSize)
        : Executors.newCachedThreadPool();

    SpecRpcStatistics.setIsEnabled(Boolean.parseBoolean(
        config.get(Constants.SPECRPC_STATISTICS_ENABLED_PROPERTY, Constants.DEFAULT_SPECRPC_STATISTICS_ENABLE)));
    SpecRpcStatistics.setIsCountingIncorrectPrediction(
        Boolean.parseBoolean(config.get(Constants.SPECRPC_STATISTICS_INCORRECT_PREDICTION_ENABLED_PROPERTY,
            Constants.DEFAULT_SPECRPC_STATISTICS_INCORRECT_PREDICTION_COUNTING)));

    terminated = false;
  }
  
  public static synchronized void setRpcSig(HashMap<String, Location> rpcSigLocTable) {
    for (String rpcSig : rpcSigLocTable.keySet()) {
      directory.setRpcSigLocation(rpcSig, rpcSigLocTable.get(rpcSig));
    }
  }
  
  public static synchronized HashMap<String, Location> getRpcSigLocationTable() {
    return directory.getRpcSigLocation();
  }

  /*
   * Interface for Caller (Client) to get the server stub for invoking RPC
   * 
   * In order to make a method call, we must first bind a method signature to an
   * IServerStub. We can then execute "call" in the stub, provide a callback that
   * process the return value of the method call, and retrieve a future that
   * encapsulates the return value of the callback.
   * 
   * this bind() is used for the first RPC, that is, there is no previous RPC
   */
  public static SpecRpcClientStub bind(String serverIdentity, RpcSignature signature)
      throws MethodNotRegisteredException, IOException {
    if (terminated == true) {
      return null;
    }
    // Given the signature, determine which server we should contact.
    Location serverLocation = lookup(serverIdentity, signature);
    // Create a ServerStube based on the method signature and the server location
    // as the initial RPC, callerStatus is SUCCEED
    return new SpecRpcServerStubObject(signature, serverLocation, clientThreadPool,
        new Status(SpeculationStatus.SUCCEED, SpeculationStatus.SUCCEED), null);
  }

  /*
   * Look up the directory for getting the server location the specific signature
   * maps to
   * 
   * NOTE: This method is public because it is also used in the bind() method in
   * SpecRPCFacade
   */
  public static Location lookup(String serverIdentity, RpcSignature signature)
      throws MethodNotRegisteredException, FileNotFoundException, IOException {
    if (terminated == true) {
      return null;
    }

    if (directory == null) {
      // Client.initClient();
      // TODO change to uninitialized exception
      System.err.println("SpecRPC Client does not initialize.");
      throw new MethodNotRegisteredException(null);
    }
    Location serverLocation = directory.lookUp(serverIdentity, signature);
    return serverLocation;
  }

  // shutdown the thread pool
  public static synchronized void shutdown() {
    if (terminated == true) { // has been terminated by other thread
      return;
    }

    try {
      // shutdown communication module
      Communication.shutdown();

      clientThreadPool.shutdown();
      if (!clientThreadPool.awaitTermination(60, TimeUnit.SECONDS)) {
        clientThreadPool.shutdownNow();
      }
    } catch (InterruptedException | IOException e) {
      e.printStackTrace();
    } finally {
      terminated = true;
    }
  }
}
