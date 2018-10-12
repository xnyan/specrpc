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

package specrpc.iterativeMultiServers;

import specrpc.client.api.SpecRpcCallbackObject;
import specrpc.client.api.SpecRpcCallbackFactory;

public class IterMockCallbackFactory implements SpecRpcCallbackFactory {

  private final String clientID;
  private final Transaction tran;
  // index of the next operation in transaction
  private final int nextOpIndex;
  private final String testTag;

  public IterMockCallbackFactory(String id, Transaction tran, int nextOpIndex, String testTag) {
    this.clientID = id;
    this.tran = tran;
    this.nextOpIndex = nextOpIndex;
    this.testTag = testTag;
  }

  @Override
  public SpecRpcCallbackObject createCallback() {
    return new IterMockCallback(this.clientID, this.tran, this.nextOpIndex, this.testTag);
  }

}
