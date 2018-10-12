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

import Waterloo.MultiSocket.IConnection;

public class ServerSimRPC extends Thread {

  private IConnection connection;

  public ServerSimRPC(IConnection connection) {
    this.connection = connection;
    start();
  }

  public void run() {
    // long start = 0;
    try {
      // start = System.nanoTime();
      this.connection.readMessage();
      // System.out.println("server read " + (System.nanoTime() - start) / 1000000.0);

      // start = System.nanoTime();
      this.connection.writeMessage(Server.ACK);
      // System.out.println("server write " + (System.nanoTime() - start) /
      // 1000000.0);

    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      try {
        // start = System.nanoTime();
        this.connection.close();
        // System.out.println("server close " + (System.nanoTime() - start) /
        // 1000000.0);
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

}
