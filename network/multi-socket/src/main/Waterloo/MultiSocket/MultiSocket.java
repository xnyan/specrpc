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
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.concurrent.Future;

import Waterloo.MultiSocket.exception.ChannelUsedException;
import Waterloo.MultiSocket.exception.ConnectionCloseException;
import Waterloo.MultiSocket.exception.ConnectionNonExistException;
import Waterloo.MultiSocket.exception.DataTooBigException;
import Waterloo.MultiSocket.exception.EndOfStreamException;
import Waterloo.MultiSocket.exception.InvalidMagicNumException;
import Waterloo.MultiSocket.exception.MultiSocketValidException;
import Waterloo.MultiSocket.exception.UnexpectedDataException;

/*
 * TODO:
 * 1. Currently, after one side A sends data, before the other side B receives the data,
 * 		B closes the connection; then A does not know that B never receives the data;
 * 		One simple solution: add sequence number to the head of message, and when close
 * 		happens, the close message contains the sequece number of the last message received
 */
public abstract class MultiSocket {
  // Different connection states.
  enum ConnectionState {
    NORMAL, RECVED_CLOSE, SENT_CLOSE, CLOSED
  }

  // Message types. Easier to use constants instead of an enum.
  protected static final int CREATE = 0;
  public static final int DATA = 1;
  protected static final int CLOSE = 2;

  private int magicNumber = 0xDEADBEEF;
  private static final int bufferSize = 64 * 1024;
  private static final int headerSize = 16;

  private byte[] messageArray;
  private ByteBuffer readBuffer;
  private ByteBuffer writeBuffer;

  private LinkedList<ChannelMessage> writeQueue;
  protected HashMap<Integer, Connection> connectionMap;

  private SocketChannel channel;
  private Selector selector;
  private boolean valid = true;
  private boolean writeRegistered = false;

  public MultiSocket(SocketChannel channel, Selector selector) {
    readBuffer = ByteBuffer.allocate(bufferSize);
    writeBuffer = ByteBuffer.allocate(bufferSize);
    writeBuffer.flip(); // Start the write buffer as empty.

    messageArray = new byte[bufferSize];
    writeQueue = new LinkedList<ChannelMessage>();
    connectionMap = new HashMap<Integer, Connection>();
    this.channel = channel;
    this.selector = selector;
  }

  public void forceClose() throws IOException {
    channel.close();
  }

  public synchronized void sendClose(Connection connection)
      throws IOException, MultiSocketValidException, ConnectionCloseException {
    writeMessage(connection, CLOSE, null);
    if (connection.closeHelper()) {
      this.connectionMap.remove(connection.getChannelNumber());
    }
  }

  public synchronized void writeMessage(Connection connection, int messageType, String message)
      throws IOException, MultiSocketValidException, ConnectionCloseException {
    if (!valid) {
      throw new MultiSocketValidException("MultiSocket is not valid");
    }
    ConnectionState state = connection.getState();
    if (state == ConnectionState.CLOSED || state == ConnectionState.SENT_CLOSE) {
      throw new ConnectionCloseException("connection is colsed, connection state is " + state);
    }
    // Check if the writeBuffer and writeQueue is empty. If they are, try to write
    // the message
    // immediately instead of waiting for the select thread to write it.
    if (writeBuffer.remaining() == 0 && writeQueue.isEmpty()) {
      loadWriteBuffer(new ChannelMessage(connection.getChannelNumber(), messageType, message));
      if (!flushBuffer()) {
        return; // Done, no more to write. Don't need to add OP_WRITE to select.
      }
    } else {
      writeQueue.add(new ChannelMessage(connection.getChannelNumber(), messageType, message));
    }
    if (!writeRegistered) {
      channel.keyFor(selector).interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
      writeRegistered = true; // Only register once
      selector.wakeup();
    }
  }

  // Returns true if buffer is not empty after writing
  private boolean flushBuffer() throws IOException {
    if (writeBuffer.remaining() == 0) {
      return false;
    }
    channel.write(writeBuffer);
    // Remaining > 0 if we can't write the entire buffer this time.
    return writeBuffer.remaining() > 0;
  }

  private void loadWriteBuffer(ChannelMessage msg) {
    writeBuffer.clear();
    writeBuffer.putInt(magicNumber);
    writeBuffer.putInt(msg.getMessageType());
    writeBuffer.putInt(msg.getChannelNumber());
    if (msg.getMessageType() == DATA) {
      writeBuffer.putInt(msg.getMessage().length());
      writeBuffer.put(msg.getMessage().getBytes());
    } else {
      writeBuffer.putInt(0);
    }
    writeBuffer.flip(); // Get ready for writes.
  }

  private void flushWritesHelper() throws IOException {
    if (flushBuffer()) {
      return; // Can't write the entire buffer this time.
    }
    // System.out.println("Num write queue entries: " + writeQueue.size());
    while (!writeQueue.isEmpty()) {
      loadWriteBuffer(writeQueue.pop());
      if (flushBuffer()) {
        return; // Can't write the entire buffer this time.
      }
    }
    // writeBuffer is empty and writeQueue is empty
    channel.keyFor(selector).interestOps(SelectionKey.OP_READ);
    // channel.register(selector, SelectionKey.OP_READ);
    writeRegistered = false;
  }

  public synchronized void flushWrites() throws IOException {
    // if (!valid) throw new Exception();
    if (!valid)
      return; // Does nothing
    try {
      flushWritesHelper();
    } catch (IOException e) {
      // System.err.println("flushWrites exception");
      valid = false;
      for (Entry<Integer, Connection> cEntry : connectionMap.entrySet()) {
        cEntry.getValue().forceClose();
      }
      channel.close(); // Cancels all of this channel's keys
      throw e; // Set valid to false and rethrow the exception;
    }
  }

  protected abstract void handleNewConnection(int channelNumber) throws ChannelUsedException;

  protected abstract Future<IConnection> createConnection()
      throws IOException, MultiSocketValidException, ConnectionCloseException;

  private void readMessagesHelper() throws IOException, EndOfStreamException, InvalidMagicNumException,
      DataTooBigException, ChannelUsedException, ConnectionNonExistException, UnexpectedDataException {
    while (true) {
      // First read as much as we can from the buffer.
      int numRead = -1;
      try {
        // System.out.println("Remaining: " + readBuffer.remaining());
        // System.out.println("Position: " + readBuffer.position());
        // System.out.println("Limit: " + readBuffer.limit());
        numRead = channel.read(readBuffer);
      } catch (BufferUnderflowException e) {// TODO distinguish different kinds of expcetions and wrap them up
        System.err.println("Underflowww!!!!!!!!" + e);
        throw e;
      } catch (IOException e) {
        // System.err.println("IOException!!! " + e);
        throw e;
      }
      if (numRead == 0) {
        // System.out.println("empty");
        return; // No more to read
      }
      if (numRead == -1) {
        // System.err.println("-1 on read");// TODO handle this end of stream when
        // caused by client-side selector close
        throw new EndOfStreamException("-1 on read, end of stream while reading buffer"); // Is this check necessary?
      }

      // Begin reading the buffer by first flipping it (setting limit to
      // position and position to 0).
      readBuffer.flip();

      try {
        while (true) {
          // Check that we've read at least the entire header.
          if (readBuffer.remaining() < headerSize) {
            break; // Hasn't read enough.
          }
          // Mark before reading header. This allows us to undo
          // the reads if there isn't enough data to read.
          readBuffer.mark();

          // First check that the magic number is correct.
          int recvedMagic = readBuffer.getInt();
          if (recvedMagic != magicNumber) {
            System.err.println("Wrong magic");
            throw new InvalidMagicNumException("Invalid Magic Number"); // Wrong magic number
          }
          // Read the remaining header information.
          int messageType = readBuffer.getInt();
          int channelNumber = readBuffer.getInt();
          int messageSize = readBuffer.getInt();

          // System.out.println("Message size: " + messageSize);

          // Check if the message is too big (invalid)
          if (headerSize + messageSize > bufferSize) {
            System.err.println("Data too big");
            throw new DataTooBigException("Msg data is too big"); // Data is bigger than a buffer
          }
          // Check if we've received the whole message
          if (readBuffer.remaining() < messageSize) {
            // Not enough data. Reset to where we marked.
            readBuffer.reset();
            break;
          }
          // Read the message into the messageArray.
          if (messageSize > 0) {
            readBuffer.get(messageArray, 0, messageSize);
          }
          // Check if it is a create connection message.
          if (messageType == CREATE) {
            handleNewConnection(channelNumber);
            continue;
          }
          // Not a Create message. Get the connection.
          Connection c = connectionMap.get(channelNumber);
          if (c == null) {
            System.err.println("NULL connection");
            throw new ConnectionNonExistException("Connection does not exist in the connection map"); // Bad state,
                                                                                                      // close the
                                                                                                      // socket.
          }
          if (messageType == CLOSE) {
            if (c.remoteClose()) {
              // Done with this connection (sent and recved close)
              connectionMap.remove(channelNumber);
            }
          } else if (messageType == DATA) {
            // TODO: Ignoring closed connections for now. Should log.
            c.deliverMessage(new String(messageArray, 0, messageSize));
          } else {
            System.err.println("Unexpected data");
            throw new UnexpectedDataException("Unexpected Data with message type = " + messageType);
          }
        }
      } finally {
        // Set the position to the limit. Then set the limit to the
        // capacity. This allows more writes.
        readBuffer.compact();
        // readBuffer.clear();

        // readBuffer.compact().position(readBuffer.limit()).limit(readBuffer.capacity());
      }
    }
  }

  public synchronized void readMessages() throws IOException, EndOfStreamException, InvalidMagicNumException,
      DataTooBigException, ChannelUsedException, ConnectionNonExistException, UnexpectedDataException {
    if (!valid) {
      return;
    }
    try {
      readMessagesHelper();
    } catch (IOException e) {
      readMessageExceptionHandler();
      throw e;
    } catch (EndOfStreamException e) {
      readMessageExceptionHandler();
      throw e;
    } catch (InvalidMagicNumException e) {
      readMessageExceptionHandler();
      throw e;
    } catch (DataTooBigException e) {
      readMessageExceptionHandler();
      throw e;
    } catch (ChannelUsedException e) {
      readMessageExceptionHandler();
      throw e;
    } catch (ConnectionNonExistException e) {
      readMessageExceptionHandler();
      throw e;
    } catch (UnexpectedDataException e) {
      readMessageExceptionHandler();
      throw e;
    }
  }

  private void readMessageExceptionHandler() throws IOException {
    valid = false;
    for (Entry<Integer, Connection> cEntry : connectionMap.entrySet()) {
      cEntry.getValue().forceClose();
    }
    channel.close(); // Cancels all of this channel's keys
  }
}
