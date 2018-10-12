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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;

public class ComClient extends Client {
  public static String REQUEST = "";

  private String serverIP;
  private int serverPort;

  public ComClient(String serverIP, int port) {
    if (REQUEST.length() == 0) {
      REQUEST = "";
      for (int i = 0; i < 1024; i++) {
        REQUEST += "r";
      }
    }
    this.serverIP = serverIP;
    this.serverPort = port;
  }

  public String request() {
    String result = null;
    Socket socket = new Socket();

    BufferedReader in = null;
    PrintWriter out = null;
    try {
      socket.setTcpNoDelay(true);
      socket.connect(new InetSocketAddress(this.serverIP, this.serverPort));
      in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
      out = new PrintWriter(socket.getOutputStream(), true);

      out.println(ComClient.REQUEST);
      result = in.readLine();
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      try {
        if (in != null)
          in.close();
        if (out != null)
          out.close();
        socket.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    return result;
  }
}
