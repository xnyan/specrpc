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

package rc.common;

public class PaxosInstance {

  private int acceptNum;
  private int rejectNum;
  private final int quorumNum;

  public PaxosInstance(int quorumNum) {
    this.acceptNum = 0;
    this.rejectNum = 0;
    this.quorumNum = quorumNum;
  }

  public synchronized int voteAccept() {
    this.acceptNum++;
    return this.acceptNum;
  }

  public synchronized int voteReject() {
    this.rejectNum++;
    return this.rejectNum;
  }

  public synchronized int getAcceptNum() {
    return this.acceptNum;
  }

  public synchronized int getRejectNum() {
    return this.rejectNum;
  }

  /**
   * Check if being accepted by this Paxos instance.
   * 
   * @return Null if not determined. True if accepted. False if rejected.
   */
  public synchronized Boolean isAccept() {
    if (this.acceptNum >= this.quorumNum) {
      return true;
    }
    if (this.rejectNum >= this.quorumNum) {
      return false;
    }
    return null;
  }
}

