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

import Waterloo.MultiSocket.MultiSocketClient;

public class Runner {

  public static void main(String args[]) throws IOException {
    if (args[0].equals("server")) {
      if (args[1].equals("multi")) {
        Server server = new Server(args[2], Integer.parseInt(args[3]));
        server.start();
      } else if (args[1].equals("com")) {
        ComServer server = new ComServer(args[2], Integer.parseInt(args[3]));
        server.start();
      } else {
        System.err.println("invalid server type");
      }
    } else if (args[0].equals("client")) {
      final String serverIP = args[2];
      final int port = Integer.parseInt(args[3]);
      final int requestNum = Integer.parseInt(args[4]);
      String result = null;
      long start = 0;
      String expectedResult = "";
      for (int i = 0; i < 1024; i++) {
        expectedResult += "a";
      }
      Client client = null;
      MultiSocketClient clientSocket = null;
      if (args[1].equals("multi")) {
        clientSocket = new MultiSocketClient();
        client = new Client(clientSocket, serverIP, port);
      } else if (args[1].equals("com")) {
        client = new ComClient(serverIP, port);
      } else {
        System.err.println("invalid client type");
      }

      for (int i = 0; i < requestNum; i++) {
        start = System.nanoTime();
        result = client.request();
        System.out.println(System.nanoTime() - start);
        if (!result.equals(expectedResult)) {
          System.err.println(i + "th request expected result: " + expectedResult.length() + " : " + expectedResult);
          System.err.println(i + "th request got incorrect result: " + result.length() + " : " + result);
        }
      }
      /*
       * if (clientSocket != null) { clientSocket.close(); }
       */

      System.err.println("client end");// not output into standard I/O
    }
  }
}
