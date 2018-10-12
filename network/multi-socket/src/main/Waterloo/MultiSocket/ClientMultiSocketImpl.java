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
import java.net.InetSocketAddress;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.LinkedList;
import java.util.concurrent.Future;

import Waterloo.MultiSocket.exception.ChannelUsedException;
import Waterloo.MultiSocket.exception.ConnectionCloseException;
import Waterloo.MultiSocket.exception.MultiSocketValidException;

public class ClientMultiSocketImpl extends MultiSocket {
  // TODO: Let's not worry about running out of channel numbers for now.
  // This needs to be fixed later.
  private int nextFreeChannel = 0;
  private final InetSocketAddress address;
  private boolean connected = false;
  private LinkedList<BasicFuture<IConnection>> connectFutureList;

  public ClientMultiSocketImpl(SocketChannel channel, Selector selector, InetSocketAddress address) {
    super(channel, selector);
    this.address = address;
    connectFutureList = new LinkedList<BasicFuture<IConnection>>();
  }

  protected void handleNewConnection(int channelNumber) throws ChannelUsedException {
    // Shouldn't accept connection in client
    // TODO replace the ChannelUsedException with a better one
    throw new ChannelUsedException("Client does not handle any new connection");
  }

  public InetSocketAddress getAddress() {
    return address;
  }

  private Connection createConnectionHelper() throws IOException, MultiSocketValidException, ConnectionCloseException {
    int channelNumber = nextFreeChannel++;
    Connection connection = new Connection(channelNumber, this);
    connectionMap.put(channelNumber, connection);
    writeMessage(connection, CREATE, null);
    return connection;
  }

  protected synchronized void connectFailed() {
    for (BasicFuture<IConnection> cf : connectFutureList) {
      cf.put(null);
    }
    connectFutureList.clear();
  }

  protected synchronized void setConnected() throws Exception {
    connected = true;
    for (BasicFuture<IConnection> cf : connectFutureList) {
      cf.put(createConnectionHelper());
    }
    connectFutureList.clear();// remove the reference of Future Object from this
  }

  protected synchronized Future<IConnection> createConnection()
      throws IOException, MultiSocketValidException, ConnectionCloseException {
    if (!connected) {
      BasicFuture<IConnection> cf = new BasicFuture<IConnection>();
      connectFutureList.add(cf);
      return cf;
    }
    return new DoneFuture<IConnection>(createConnectionHelper());
  }
}
