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

package performance;

import java.net.InetSocketAddress;
import java.util.concurrent.ExecutionException;
import Waterloo.MultiSocket.IConnection;
import Waterloo.MultiSocket.MultiSocketClient;

public class Client {
  public static String REQUEST = "";

  private MultiSocketClient clientSocket;
  private String serverIP;
  private int serverPort;

  public Client() {

  }

  public Client(MultiSocketClient socket, String serverIP, int port) {
    if (REQUEST.length() == 0) {
      REQUEST = "";
      for (int i = 0; i < 1024; i++) {
        REQUEST += "r";
      }
    }
    this.clientSocket = socket;
    this.serverIP = serverIP;
    this.serverPort = port;

  }

  public String request() {
    String result = null;
    IConnection c = null;
    // long start = 0;
    try {
      // start = System.nanoTime();
      c = this.clientSocket.connect(new InetSocketAddress(serverIP, serverPort)).get();
      // System.out.println("client connect " + (System.nanoTime() - start) /
      // 1000000.0);

      // start = System.nanoTime();
      c.writeMessage(REQUEST);
      // System.out.println("client write " + (System.nanoTime() - start) /
      // 1000000.0);

      // start = System.nanoTime();
      result = c.readMessage();
      // System.out.println("client read " + (System.nanoTime() - start) / 1000000.0);
    } catch (InterruptedException e) {
      e.printStackTrace();
    } catch (ExecutionException e) {
      e.printStackTrace();
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      if (c != null) {
        try {
          // start = System.nanoTime();
          c.close();
          // System.out.println("client close " + (System.nanoTime() - start) /
          // 1000000.0);
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    }
    return result;
  }
}
