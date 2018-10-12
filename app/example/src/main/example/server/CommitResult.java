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

package example.server;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonSyntaxException;

public class CommitResult {

  public final String txnId;
  public final Boolean isCommitted;
  public final Float data;// used for customized equivalence

  public CommitResult(String txnId, Boolean isCommitted, Float data) {
    this.txnId = txnId;
    this.isCommitted = isCommitted;
    this.data = data;
  }

  // Not needed for serialization
  public CommitResult(JsonElement jsonSign) throws JsonSyntaxException, ClassNotFoundException {
    Gson gson = new Gson();
    JsonArray array = gson.fromJson(jsonSign, JsonArray.class);
    this.txnId = gson.fromJson(array.get(0), String.class);
    this.isCommitted = gson.fromJson(array.get(1), Boolean.class);
    this.data = gson.fromJson(array.get(2), Float.class);
  }

  public String toString() {
    return this.txnId + ":" + this.isCommitted + ":" + this.data;
  }

  // Customized equivalence
  public boolean equals(Object obj) {
    CommitResult cr = (CommitResult) obj;
    if (!cr.txnId.equals(this.txnId)) {
      return false;
    }
    if (cr.isCommitted != this.isCommitted) {
      return false;
    }
    if (Math.abs(cr.data - this.data) > 1) {
      return false;
    }
    return true;
  }
}
