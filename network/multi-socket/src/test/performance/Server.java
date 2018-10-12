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

import java.io.IOException;
import java.net.InetSocketAddress;

import Waterloo.MultiSocket.MultiSocketServer;

public class Server extends Thread {

  public static String ACK = "";

  private MultiSocketServer server;

  public Server(String ip, int port) {
    if (ACK.length() == 0) {
      ACK = "";
      for (int i = 0; i < 1024; i++) {
        ACK += "a";
      }
    }

    try {
      server = new MultiSocketServer(new ServerAcceptHandler(), new InetSocketAddress(ip, port));
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public void run() {
    try {
      server.accept();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
