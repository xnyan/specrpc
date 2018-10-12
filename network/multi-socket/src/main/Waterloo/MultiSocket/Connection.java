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

package Waterloo.MultiSocket;

import java.io.IOException;
import java.util.LinkedList;

import Waterloo.MultiSocket.MultiSocket.ConnectionState;
import Waterloo.MultiSocket.exception.ConnectionCloseException;
import Waterloo.MultiSocket.exception.MultiSocketValidException;

public class Connection implements IConnection {
  private final int channelNumber;
  private LinkedList<String> readQueue;
  private ConnectionState state = ConnectionState.NORMAL;
  private MultiSocket socket;

  public Connection(int channelNumber, MultiSocket socket) {
    readQueue = new LinkedList<String>();
    this.channelNumber = channelNumber;
    this.socket = socket;
  }

  public int getChannelNumber() {
    return channelNumber;
  }

  public synchronized ConnectionState getState() {
    return state;
  }

  public synchronized String readMessage() throws InterruptedException, ConnectionCloseException {
    while (readQueue.isEmpty() && state == ConnectionState.NORMAL) {
      wait(); // Block until there is something to read.
    }
    if (state != ConnectionState.NORMAL && readQueue.isEmpty()) {
      // System.out.println("Not normal state in Connection");
      throw new ConnectionCloseException("Connection closed, connection state is " + state);
    }
    return readQueue.pop();
  }

  public void writeMessage(String message) throws IOException, MultiSocketValidException, ConnectionCloseException {
    // MultiSocket.this.writeMessage(this, DATA, message);
    socket.writeMessage(this, MultiSocket.DATA, message);
  }

  public void close() throws IOException, MultiSocketValidException, ConnectionCloseException {
    socket.sendClose(this);
  }

  // Returns true if this connection should be removed.
  protected synchronized boolean remoteClose() {
    if (state == ConnectionState.SENT_CLOSE) {
      state = ConnectionState.CLOSED;
    } else {
      state = ConnectionState.RECVED_CLOSE;
    }
    notifyAll(); // Make sure we wake up any readers
    return state == ConnectionState.CLOSED;
  }

  // return true if connection is closed
  protected synchronized boolean closeHelper() {
    if (state == ConnectionState.RECVED_CLOSE) {
      state = ConnectionState.CLOSED;
    } else {
      state = ConnectionState.SENT_CLOSE;
    }
    notifyAll(); // Make sure we wake up any readers.
    return ConnectionState.CLOSED == state;
  }

  protected synchronized boolean deliverMessage(String message) {
    // System.out.println("deliverMessage");
    if (state != ConnectionState.NORMAL) {
      return false;
    }
    readQueue.add(message);
    notify(); // Wakeup just one blocking thread.
    return true;
  }

  protected synchronized void forceClose() {
    // System.out.println("forceClose");
    state = ConnectionState.CLOSED;
    notifyAll();
  }
}