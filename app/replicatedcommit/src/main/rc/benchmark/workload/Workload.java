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

package rc.benchmark.workload;

import java.util.Random;

import rc.client.txn.ClientTxnOperation;
import rc.common.RcConstants;
import utils.ZipfGenerator;

public abstract class Workload {

  protected String[] keyList;
  protected ZipfGenerator zipfGenerator;
  protected Random rnd;
  
  public Workload(
      String[] keyList,
      ZipfGenerator zipfGenerator,
      long randomSeed) {
    this.keyList = keyList;
    this.zipfGenerator = zipfGenerator;
    this.rnd = new Random(randomSeed);
  }
  
  /**
   * Generates a transaction that consists of multiple read and write operations.
   * 
   * @return a list of ClientTxnOperation
   */
  public abstract ClientTxnOperation[] nextTxn();
  
  protected String genKey() {
    int keyIdx = this.zipfGenerator.sample() - 1; // [0, length)
    return keyList[keyIdx];
  }
  
  protected String genVal() {
    String val = this.rnd.nextInt(RcConstants.PERCENTAGE) + ""; // TODO Changes to the string value in YCSB
    return val;
  }
}
