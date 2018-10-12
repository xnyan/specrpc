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

import rpc.communication.Message;

import com.google.gson.Gson;

public class CallerSpeculationSolvedMsg implements Message {

  public enum Resolution {
    COMMIT, ABORT
  }

  public final Resolution resolType;

  public CallerSpeculationSolvedMsg(Resolution type) {
    this.resolType = type;
  }

  public CallerSpeculationSolvedMsg(String message) {
    this.resolType = new Gson().fromJson(message, Resolution.class);
  }

  @Override
  public String serialize() {
    return new Gson().toJson(resolType);
  }
}
