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

import Waterloo.MultiSocket.ConnectionHandler;
import Waterloo.MultiSocket.IConnection;
import Waterloo.MultiSocket.MultiSocketServer;
import Waterloo.MultiSocket.exception.ConnectionCloseException;

public class TestServer extends Thread {
  public class TestReader extends Thread {
    private IConnection connection;

    public TestReader(IConnection connection) {
      this.connection = connection;
      start();
    }

    public void run() {
      try {
        // while (true) {
        String recved = connection.readMessage();
        // System.out.println("Server Received: " + recved);
        connection.writeMessage("1st Ack: " + recved);
        connection.writeMessage("2nd Ack: " + recved);
        connection.writeMessage("3rd Ack: " + recved);
        // }
      } catch (Exception e) {
        e.printStackTrace();
        if (e instanceof ConnectionCloseException) {
          System.out.println("Server side: " + e.getMessage());
        } else {
          System.out.println("Connection closed: " + e);
        }
        try {
          connection.close();
          connection = null;
        } catch (Exception e1) {
          e1.printStackTrace();
        }
      } finally {
        try {
          if (connection != null) {
            connection.close();
          }
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    }
  }

  public class TestAcceptHandler implements ConnectionHandler {
    public void handle(IConnection connection) {
      // System.out.println("IN TestAcceptHandler");
      new TestReader(connection);
    }
  }

  private MultiSocketServer server;

  public TestServer() throws IOException {
    server = new MultiSocketServer(new TestAcceptHandler(), new InetSocketAddress("127.0.0.1", 1234));
    start();
    System.out.println("Server is starting...");
  }

  public void run() {
    try {
      server.accept();
    } catch (IOException e) {
      System.out.println("Received IOException in TestServer: " + e);
    }
  }
}
