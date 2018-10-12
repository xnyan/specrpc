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

package specrpc.communication;

import java.util.ArrayList;
import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rpc.communication.Message;
import rpc.config.Constants;
import specrpc.common.RpcSignature;
import specrpc.common.Status.SpeculationStatus;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

public class RequestMsg implements Message {

  protected static final Logger logger = LoggerFactory.getLogger(Constants.LOGGER_TYPE);

  public final RpcSignature signature;
  public final Object[] args;
  public final SpeculationStatus callerStatus;

  public RequestMsg(SpeculationStatus callerStatus, RpcSignature signature, Object... args) {
    this.callerStatus = callerStatus;
    this.signature = signature;
    this.args = args;
  }

  public RequestMsg(String message) throws JsonSyntaxException, ClassNotFoundException {
    JsonParser parser = new JsonParser();
    JsonArray array = parser.parse(message).getAsJsonArray();
    this.signature = new RpcSignature(array.get(0));
    this.args = this.signature.parseArgs(array.get(1));
    this.callerStatus = new Gson().fromJson(array.get(2), SpeculationStatus.class);
  }

  @Override
  public String serialize() {
    if (callerStatus == SpeculationStatus.FAIL) {
      logger.error("Should not send a message under FAIL speculation status. Check errors!");
      assert (callerStatus != SpeculationStatus.FAIL);
    }

    Collection<Object> msg = new ArrayList<Object>();

    msg.add(signature.toArray());
    msg.add(args);
    msg.add(callerStatus);

    return new Gson().toJson(msg);
  }

}
