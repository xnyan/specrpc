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

package rc.client.specrpc;

import java.util.Hashtable;

import rc.client.txn.ClientTxnOperation;
import specrpc.client.api.SpecRpcCallback;
import specrpc.client.api.SpecRpcCallbackFactory;

public class ReadCallbackFactory implements SpecRpcCallbackFactory {

  public final String txnId;
  public final String readKey; // The read key of the RPC that triggers this callback.
  protected final ClientTxnOperation[] txnOpList;
  protected final int opStartIndex; // The index of the operation that should be performed

  protected final Hashtable<String, String> readKeyValTable; // thread safe
  protected final Hashtable<String, String> writeKeyValTable; // thread safe

  public ReadCallbackFactory(
      String txnId,
      String readKey,
      ClientTxnOperation[] txnOpList,
      int opStartIndex,
      Hashtable<String, String> readKeyValTable,
      Hashtable<String, String> writeKeyValTable) {
    this.txnId = txnId;
    this.readKey = readKey;
    this.txnOpList = txnOpList;
    this.opStartIndex = opStartIndex;
    this.readKeyValTable = readKeyValTable;
    this.writeKeyValTable = writeKeyValTable;
  }
  
  @Override
  public SpecRpcCallback createCallback() {
    return new ReadCallback(
        this.txnId, 
        this.readKey, 
        this.txnOpList, 
        this.opStartIndex, 
        this.readKeyValTable, 
        this.writeKeyValTable
        );
  }

}
