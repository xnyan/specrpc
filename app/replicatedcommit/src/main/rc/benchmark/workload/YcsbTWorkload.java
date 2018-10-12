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

import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rc.client.txn.ClientTxnOperation;
import rc.client.txn.ReadOperation;
import rc.client.txn.WriteOperation;
import rc.common.RcConstants;
import utils.ZipfGenerator;

/**
 * YcsbTWorkload implements the YCSB workload for read, write, and read-modify-write operations
 * with a Zipf distribution (including uniform distribution when Zipf = 0).
 * 
 * This is mainly because YCSB (access in Sep. 2017) uses a SCRAMBLED Zipf distribution 
 * When the number of records is no 10 million, YCSB is not using an exact Zipf distribution.
 * 
 */
public class YcsbTWorkload extends Workload {

  private static final Logger logger = LoggerFactory.getLogger(RcConstants.LOGGER_TYPE);
  
  private final int opNum; // the number of operations per transaction
  private final int readPortionCap; // read portion cap [0, 100]
  private final int writePortionCap; // write portion cap [0, 100] = read + write portion
  private final int rmwPortionCap; // read-modify-write portion cap = 100
  
  public YcsbTWorkload(
      int opNum, // the number of operations per transaction
      int readP, // read portion [0, 100]
      int writeP, // write portion [0, 100]
      int rmwP, // read-modify-write portion [0, 100]
      String[] keyList,
      ZipfGenerator zipfGenerator,
      long randomSeed) {
    
    super(keyList, zipfGenerator, randomSeed);
    
    this.opNum = opNum;
    this.readPortionCap = readP;
    this.writePortionCap = this.readPortionCap + writeP;
    this.rmwPortionCap = this.writePortionCap + rmwP;
    
    if (this.rmwPortionCap != 100) {
      logger.error("Invalid read, write, and rmw portion set up for YCSB+T.");
      System.exit(RcConstants.INIT_FAIL_ERROR_CODE);
    }
  }

  @Override
  public ClientTxnOperation[] nextTxn() {
    ArrayList<ClientTxnOperation> txnOpList = new ArrayList<ClientTxnOperation>();
    for (int i = 0; i < this.opNum; i++) {
      String key = this.genKey();
      
      int percentage = this.rnd.nextInt(RcConstants.PERCENTAGE); //[0, 100)
      if (percentage < this.readPortionCap) {
        // read
        txnOpList.add(new ReadOperation(key));
      } else if (percentage < this.writePortionCap) {
        // write
        String val = this.genVal();
        txnOpList.add(new WriteOperation(key, val));
      } else {
        // read-modify-write
        txnOpList.add(new ReadOperation(key));
        String val = this.genVal();
        txnOpList.add(new WriteOperation(key, val));
      }
    }
    return txnOpList.toArray(RcConstants.CLIENT_TXN_OPERATION_ARRAY_TYPE_HELPER);
  }
}
