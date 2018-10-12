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
import java.net.ServerSocket;
import java.net.Socket;

public class ComServer extends Thread {

  public static String ACK = "";

  private ServerSocket listener;

  public ComServer(String ip, int port) {
    if (ACK.length() == 0) {
      ACK = "";
      for (int i = 0; i < 1024; i++) {
        ACK += "a";
      }
    }

    try {
      this.listener = new ServerSocket();
      this.listener.setReuseAddress(true);
      this.listener.bind(new InetSocketAddress(ip, port), 1024);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public void run() {
    while (true) {
      try {
        Socket socket = this.listener.accept();
        socket.setTcpNoDelay(true);
        new ComServerSimRPC(socket);
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }
}
