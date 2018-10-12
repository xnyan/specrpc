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

package test;

import java.io.IOException;
import java.net.InetSocketAddress;

import Waterloo.MultiSocket.IConnection;
import Waterloo.MultiSocket.MultiSocketClient;
import Waterloo.MultiSocket.exception.ConnectionCloseException;

public class TestClient extends Thread {
  private MultiSocketClient client;
  private int id;

  public TestClient(MultiSocketClient client, int id) {
    this.client = client;
    this.id = id;
    start();
  }

  public void run() {
    IConnection c = null;
    try {
      // test sending 1KB data multiple times
      String OneKBData = "";
      for (int i = 0; i < 1024; i++) {
        OneKBData += "a";
      }

      System.out.println("Connecting to server @ client " + id);
      for (int i = 0; i < 10000; ++i) {
        // System.out.println("client " + id + " " + i + "th run");
        c = client.connect(new InetSocketAddress("127.0.0.1", 1234)).get();

        String str = "Hello world: " + id + " " + i + OneKBData;
        c.writeMessage(str);

        if (!c.readMessage().equals("1st Ack: " + str)) {
          System.out.println("1st Ack is incorrect");
        }
        if (!c.readMessage().equals("2nd Ack: " + str)) {
          System.out.println("2nd Ack is incorrect");
        }
        // c.readMessage();//Exception should happen here

        try {
          // System.out.println("close connection");
          if (c != null) {
            c.close();
            c = null;
          }
        } catch (Exception e) {
          System.out.println("close connection exception e:" + e.getMessage());
        }
      }

    } catch (ConnectionCloseException e) {
      System.out.println("Client side " + e.getMessage());
    } catch (Exception e) {
      System.out.println("exception e:" + e.getMessage());
      e.printStackTrace();
    } finally {
      if (c != null) {
        try {
          c.close();
        } catch (Exception e) {
          System.out.println("close connection exception e:" + e.getMessage());
        }
      }

      try {
        this.client.close();
      } catch (IOException e) {
        e.printStackTrace();
      }

      System.out.println("client " + this.id + " ends");
    }
  }
}
