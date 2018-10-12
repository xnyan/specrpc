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

package rpc.communication;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutionException;

import Waterloo.MultiSocket.IConnection;
import Waterloo.MultiSocket.MultiSocketClient;
import Waterloo.MultiSocket.exception.ConnectionCloseException;
import Waterloo.MultiSocket.exception.MultiSocketValidException;
import specrpc.common.Location;

public class Communication {

  private final IConnection connection;

  public Communication(IConnection connection) {
    this.connection = connection;
  }

  // TODO: disconnect does not need to throw close Exception if it is already closed.
  public void disconnect() throws IOException, MultiSocketValidException, ConnectionCloseException {
    this.connection.close();
  }

  public void send(String msg) throws IOException, MultiSocketValidException, ConnectionCloseException {
    this.connection.writeMessage(msg);
  }

  public String getMessage() throws InterruptedException, ConnectionCloseException {
    return this.connection.readMessage();
  }

  private static MultiSocketClient clientSocket = null;

  public static synchronized void initClientCommunication() throws IOException {
    if (clientSocket == null) {
      clientSocket = new MultiSocketClient();
    }
  }

  public static Communication connectTo(Location serverLocation) throws IOException, InterruptedException,
      ExecutionException, MultiSocketValidException, ConnectionCloseException {
    IConnection connection = clientSocket
        .connect(new InetSocketAddress(serverLocation.hostname, serverLocation.port))
        .get();

    if (connection == null) {
      return null;
    }

    return new Communication(connection);
  }

  public static synchronized void shutdown() throws IOException {
    if (clientSocket != null) {
      clientSocket.close();
      clientSocket = null;
    }
  }
}
