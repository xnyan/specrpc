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
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;
import java.util.concurrent.Future;

import Waterloo.MultiSocket.exception.ConnectionCloseException;
import Waterloo.MultiSocket.exception.MultiSocketValidException;

public class MultiSocketClient extends Thread {
  private LinkedList<SocketChannel> registerList;
  private HashMap<InetSocketAddress, ClientMultiSocketImpl> addressMap;
  private HashMap<SocketChannel, ClientMultiSocketImpl> socketMap;
  private Selector selector;
  private boolean done = false;

  public MultiSocketClient() throws IOException {
    selector = Selector.open();
    registerList = new LinkedList<SocketChannel>();
    addressMap = new HashMap<InetSocketAddress, ClientMultiSocketImpl>();
    socketMap = new HashMap<SocketChannel, ClientMultiSocketImpl>();
    start();
  }

  public synchronized Future<IConnection> connect(InetSocketAddress address)
      throws IOException, MultiSocketValidException, ConnectionCloseException {
    ClientMultiSocketImpl socket = addressMap.get(address);
    if (socket == null) {
      SocketChannel client = SocketChannel.open();
      client.socket().setTcpNoDelay(true);
      client.configureBlocking(false);
      client.connect(address);
      socket = new ClientMultiSocketImpl(client, selector, address);
      addressMap.put(address, socket);
      socketMap.put(client, socket);
      registerList.add(client);
      selector.wakeup();
      return socket.createConnection();
    }
    return socket.createConnection();
  }

  public void close() throws IOException {
    setDone();
    try {
      selector.close();
    } catch (IOException e) {
      throw e;
    }
  }

  private synchronized void setDone() {
    done = true;
  }

  private synchronized boolean isDone() {
    return done;
  }

  private synchronized void removeSocket(SocketChannel client) {
    ClientMultiSocketImpl sock = socketMap.get(client);
    if (sock == null) {
      assert (false);
      return;
    }
    addressMap.remove(sock.getAddress());
    socketMap.remove(client);
  }

  private synchronized ClientMultiSocketImpl getSocket(SocketChannel client) {
    return socketMap.get(client);
  }

  // Convenience function
  private void fullyRemoveKey(SelectionKey key) {
    key.cancel(); // Have to manually remove the key.
    SocketChannel client = (SocketChannel) key.channel();
    ClientMultiSocketImpl socket = getSocket(client);
    assert (socket != null);
    socket.connectFailed();
    removeSocket(client);
  }

  private synchronized void registerKey() {
    for (SocketChannel client : registerList) {
      try {
        // System.out.println("Registering connect");
        client.register(selector, SelectionKey.OP_CONNECT);
        // System.out.println("... finished");
      } catch (ClosedChannelException e) {
        fullyRemoveKey(client.keyFor(selector));
      }
    }
    registerList.clear();
  }

  // Should not directly access any fields except for selector.
  public void run() {
    while (!isDone()) {
      /*
       * System.out.println("Client Multi Socket addressMap size " +
       * this.addressMap.size()); for (Entry<InetSocketAddress,ClientMultiSocketImpl>
       * entry : this.addressMap.entrySet()) {
       * System.out.println("Client Multi Socket connection Map Size " +
       * entry.getValue().connectionMap.size()); }
       * System.out.println("Client Multi Socket socketMap size " +
       * this.socketMap.size()); for (Entry<SocketChannel,ClientMultiSocketImpl> entry
       * : this.socketMap.entrySet()) {
       * System.out.println("Client Multi Socket connection Map Size " +
       * entry.getValue().connectionMap.size()); }
       */
      try {
        try {
          selector.select();
        } catch (IOException e) {
          setDone();
          return; // Done with this thread
        }
        Set<SelectionKey> keys = selector.selectedKeys();
        Iterator<SelectionKey> it = keys.iterator();

        // Register new clients
        registerKey();

        while (it.hasNext()) {
          // System.out.println("In client's hasNext");
          SelectionKey key = it.next();
          it.remove(); // Need to manually remove this key.
          // Check what type of operation we are doing.
          if (key.isConnectable()) {
            SocketChannel client = (SocketChannel) key.channel();
            assert (client.isConnectionPending());
            try {
              if (client.finishConnect()) {
                // System.out.println("Finished connect");
                key.interestOps(SelectionKey.OP_READ);
                ClientMultiSocketImpl socket = getSocket(client);
                assert (socket != null);
                socket.setConnected();
              }
            } catch (Exception e) {
              fullyRemoveKey(key);
            }
            continue; // Will never be connectable and readable/writable.
          } else if (key.isReadable()) {
            SocketChannel client = (SocketChannel) key.channel();
            MultiSocket socket = getSocket(client);
            assert (socket != null);
            try {
              socket.readMessages();
            } catch (Exception e) {
              removeSocket(client);
              continue; // Channel should already be closed and cancelled
            }
          } else if (key.isWritable()) {
            // System.out.println("In writable");
            SocketChannel client = (SocketChannel) key.channel();
            MultiSocket socket = getSocket(client);
            assert (socket != null);
            try {
              socket.flushWrites();
            } catch (Exception e) {
              removeSocket(client);
              continue; // Channel should already be closed and cancelled
            }
          }
        }
      } catch (ClosedSelectorException e) {
        assert (isDone());
        return;
      }
    }
  }
}
