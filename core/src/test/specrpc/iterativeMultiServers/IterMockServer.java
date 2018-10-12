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

package specrpc.iterativeMultiServers;

import java.io.IOException;

import rpc.execption.UninitializationException;
import specrpc.server.api.SpecRpcServer;

public class IterMockServer implements Runnable {

  public static final String OPERATION_RESULT_SEPERATOR = ":";
  public static final String PUT_PREDICT_ALWAYS_TRUE = "always_true";
  public static final String PUT_PREDICT_ALWAYS_FALSE = "always_false";
  public static final String PUT_PREDICT_RANDOM_VALUE = "random_value";

  private final String serverID;
  private final Database database;
  private final SpecRpcServer specRpcServer;

  public IterMockServer(String id) {
    this.serverID = id;
    this.database = Database.getDatabase(this.serverID + "");
    this.specRpcServer = new SpecRpcServer();
    // init some value for testing
    for (int i = 0; i < 10; i++) {
      this.database.put(i + "", i + "", i);
    }

    // test
    System.out.println("[DEBUG INFO] ST Server " + this.serverID + " is Starting...");

    initSpecRPCModule();

    // test
    System.out.println("[DEBUG INFO] ST Server " + this.serverID + " Started.");
  }

  private void initSpecRPCModule() {
    try {
      specRpcServer.initServer(this.serverID, null);
      registerMethods();
    } catch (IOException e) {
      e.printStackTrace();
    } catch (UninitializationException e) {
      e.printStackTrace();
    }

  }

  private void registerMethods() throws UninitializationException {
    System.out.println("[DEBUG INFO] Register the RPC methods of server: " + this.serverID);

    IterMockServiceFactory serviceFactory = new IterMockServiceFactory(this.serverID, this.database);
    specRpcServer.register("getValue", serviceFactory, String.class, String.class, Long.class);
    specRpcServer.register("putValue", serviceFactory, Boolean.class, String.class, String.class, Long.class);
  }

  @Override
  public void run() {
    try {
      specRpcServer.execute();// this thread will block here
    } catch (UninitializationException | InterruptedException | IOException e) {
      e.printStackTrace();
    }
  }

  public void terminate() {
    try {
      specRpcServer.terminate();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
