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
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.Future;

import Waterloo.MultiSocket.exception.ChannelUsedException;
import Waterloo.MultiSocket.exception.ConnectionCloseException;
import Waterloo.MultiSocket.exception.ConnectionNonExistException;
import Waterloo.MultiSocket.exception.DataTooBigException;
import Waterloo.MultiSocket.exception.EndOfStreamException;
import Waterloo.MultiSocket.exception.InvalidMagicNumException;
import Waterloo.MultiSocket.exception.MultiSocketValidException;
import Waterloo.MultiSocket.exception.UnexpectedDataException;

public class MultiSocketServer {
  public class ServerMultiSocketImpl extends MultiSocket {
    public ServerMultiSocketImpl(SocketChannel channel, Selector selector) {
      super(channel, selector);
    }

    protected synchronized void handleNewConnection(int channelNumber) throws ChannelUsedException {
      if (connectionMap.containsKey(channelNumber)) {
        throw new ChannelUsedException("Channel Number " + channelNumber + " is being used"); // Channel number already
                                                                                              // used.
      }
      Connection connection = new Connection(channelNumber, this);
      connectionMap.put(channelNumber, connection);
      // Presumably, the handler will create a thread to handle each connection.
      acceptHandler.handle(connection);
    }

    protected Future<IConnection> createConnection()
        throws IOException, MultiSocketValidException, ConnectionCloseException {
      // Shouldn't allow new connections to be created.
      // TODO replace a better exception
      throw new MultiSocketValidException("ServerMultiSocket does not create new connection");
    }
  }

  private ConnectionHandler acceptHandler;
  private HashMap<SocketChannel, MultiSocket> socketMap;
  private ServerSocketChannel server;
  private Selector selector;

  public MultiSocketServer(ConnectionHandler acceptHandler, InetSocketAddress addr) throws IOException {
    init(acceptHandler, addr, -1);
  }

  public MultiSocketServer(ConnectionHandler acceptHandler, InetSocketAddress addr, int backlog) throws IOException {
    init(acceptHandler, addr, backlog);
  }

  private void init(ConnectionHandler acceptHandler, InetSocketAddress addr, int backlog) throws IOException {
    this.acceptHandler = acceptHandler;
    socketMap = new HashMap<SocketChannel, MultiSocket>();
    server = ServerSocketChannel.open();
    server.configureBlocking(false);
    server.socket().setReuseAddress(true);
    // dynamic port will be bound when the port in addr is 0
    if (backlog > 0) {
      server.socket().bind(addr, backlog);
    } else {
      server.socket().bind(addr);// default backlog is 50
    }
    selector = Selector.open();
    server.register(selector, SelectionKey.OP_ACCEPT);
  }

  // when dynamic port is generated when binding server socket
  public int getLocalPort() {
    return this.server.socket().getLocalPort();
  }

  // TODO: One minor optimization we can do is to perform multiple accepts on each
  // select iteration
  public void accept() throws IOException {
    while (true) {
      selector.select(); // Wait for events
      Set<SelectionKey> keys = selector.selectedKeys();
      Iterator<SelectionKey> it = keys.iterator();

      while (it.hasNext()) {
        SelectionKey key = it.next();
        it.remove(); // Need to manually remove this key.

        // Check what type of operation we are doing.
        if (key.isAcceptable()) {
          SocketChannel client = server.accept();
          client.socket().setTcpNoDelay(true);
          client.configureBlocking(false);
          client.register(selector, SelectionKey.OP_READ);
          socketMap.put(client, new ServerMultiSocketImpl(client, selector));
          continue;
        } else if (key.isReadable()) {
          SocketChannel client = (SocketChannel) key.channel();
          MultiSocket socket = socketMap.get(client);
          assert (socket != null);
          try {
            socket.readMessages();
          } catch (IOException e) {
            socketMap.remove(client);
            continue; // Channel should already be closed and cancelled
          } catch (EndOfStreamException e) {
            socketMap.remove(client);
            continue; // Channel should already be closed and cancelled
          } catch (InvalidMagicNumException e) {
            socketMap.remove(client);
            continue; // Channel should already be closed and cancelled
          } catch (DataTooBigException e) {
            socketMap.remove(client);
            continue; // Channel should already be closed and cancelled
          } catch (ChannelUsedException e) {
            socketMap.remove(client);
            continue; // Channel should already be closed and cancelled
          } catch (ConnectionNonExistException e) {
            socketMap.remove(client);
            continue; // Channel should already be closed and cancelled
          } catch (UnexpectedDataException e) {
            socketMap.remove(client);
            continue; // Channel should already be closed and cancelled
          }
        } else if (key.isWritable()) {
          SocketChannel client = (SocketChannel) key.channel();
          MultiSocket socket = socketMap.get(client);
          assert (socket != null);
          try {
            socket.flushWrites();
          } catch (Exception e) {
            if (!(e instanceof IOException)) {// TODO distinguish client close and other exceptions
              e.printStackTrace();
            }
            socketMap.remove(client);
            continue; // Channel should already be closed and cancelled
          }
        }
      }
    }
  }

  public void close() throws IOException {
    this.selector.close();
    this.server.close();
  }
}
