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

package micro.client.request;

import java.util.ArrayList;

public class MicroRequest {

  public enum MICRO_REQUEST_TYPE {
    ONE_HOP, MULTI_HOP
  }

  public final String id;
  private final MICRO_REQUEST_TYPE reqType;
  private final String requestData;
  private final ArrayList<Integer> hopNumList;
  private final ArrayList<Long> compTimeList;

  public MicroRequest(String id, MICRO_REQUEST_TYPE reqType, String reqData) {
    this.id = id;
    this.reqType = reqType;
    this.requestData = reqData;
    this.hopNumList = new ArrayList<Integer>();
    this.compTimeList = new ArrayList<Long>();
  }

  public MICRO_REQUEST_TYPE getType() {
    return this.reqType;
  }
  
  public String getData() {
    return this.requestData;
  }
  
  public void addRpc(int hopNum, long localCompTime) {
    this.hopNumList.add(hopNum);
    this.compTimeList.add(localCompTime);
  }
  
  public int getRpcNum() {
    return this.hopNumList.size();
  }
  
  /**
   * Gets the hop number given the ith RPC
   * @param i, the ith RPC, where i is [1, n]
   * @return
   */
  public int getRpcHopNum(int i) {
    return this.hopNumList.get(i - 1);
  }
  
  public long getLocalCompTime(int i) {
    return this.compTimeList.get(i - 1);
  }
}
