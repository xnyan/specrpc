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

package tradrpc.client;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import Waterloo.MultiSocket.exception.ConnectionCloseException;
import Waterloo.MultiSocket.exception.MultiSocketValidException;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;

import rpc.communication.Communication;
import rpc.communication.Message;
import specrpc.common.Location;
import specrpc.common.RpcSignature;
import tradrpc.client.api.TradRpcServerStub;
import tradrpc.communication.TradRpcRequestMsg;
import tradrpc.communication.TradRpcResponseMsg;

public class TradRpcServerStubObject implements TradRpcServerStub {

  private final RpcSignature signature;
  private final Location serverLocation;
  private Communication comChannel;

  public TradRpcServerStubObject(RpcSignature signature, Location serverLocation) {
    this.signature = signature;
    this.serverLocation = serverLocation;
  }

  @Override
  public Object call(Object... args) throws TradRpcUserException {
    Object result = null;
    try {

      this.comChannel = Communication.connectTo(serverLocation);
      Message msg = new TradRpcRequestMsg(this.signature, args);
      comChannel.send(msg.serialize());
      String returnMsg = comChannel.getMessage();

      Gson gson = new Gson();
      JsonParser parser = new JsonParser();
      JsonArray array = parser.parse(returnMsg).getAsJsonArray();
      TradRpcResponseMsg.MessageType type = gson.fromJson(array.get(0), TradRpcResponseMsg.MessageType.class);

      if (type == TradRpcResponseMsg.MessageType.EXCEPTION) {
        result = gson.fromJson(array.get(1), String.class);
        throw new TradRpcUserException(result.toString());
      }
      result = gson.fromJson(array.get(1), this.signature.returnType);

      // Socket connection is closed in "finally"
    } catch (IOException | InterruptedException | ExecutionException | MultiSocketValidException | ConnectionCloseException e) {
      e.printStackTrace();
    } finally {
      try {
        if (this.comChannel != null) {
          this.comChannel.disconnect();
        }
      } catch (IOException | MultiSocketValidException | ConnectionCloseException e) {
        e.printStackTrace();
      }
    }

    return result;
  }

  @Override
  public Location getServerLocation() {
    return this.serverLocation;
  }

}
