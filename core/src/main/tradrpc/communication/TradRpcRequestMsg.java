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

package tradrpc.communication;

import java.util.ArrayList;
import java.util.Collection;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import rpc.communication.Message;
import specrpc.common.RpcSignature;

public class TradRpcRequestMsg implements Message {

  public final RpcSignature signature;
  public final Object[] args;

  public TradRpcRequestMsg(RpcSignature signature, Object... args) {
    this.signature = signature;
    this.args = args;
  }

  public TradRpcRequestMsg(String message) throws JsonSyntaxException, ClassNotFoundException {
    JsonParser parser = new JsonParser();
    JsonArray array = parser.parse(message).getAsJsonArray();
    this.signature = new RpcSignature(array.get(0));
    this.args = this.signature.parseArgs(array.get(1));
  }

  @Override
  public String serialize() {
    Collection<Object> msg = new ArrayList<Object>();

    msg.add(signature.toArray());
    msg.add(args);

    return new Gson().toJson(msg);
  }

}
