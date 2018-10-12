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

package rc.server.db.txn;

import java.util.concurrent.ArrayBlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rc.common.RcConstants;
import rc.common.TxnInfo;
import rc.server.RcServer;

public class RcTxnPrepareRunner implements Runnable {

  private static final Logger logger = LoggerFactory.getLogger(RcConstants.LOGGER_TYPE);
  
  private final String serverId;
  private final TxnInfo txnInfo;
  private final ArrayBlockingQueue<RcTxnPrepareResult> prepareResultQueue;
  private final RcTxnCoordinatorRpcFacade rpcFacade;
  
  public RcTxnPrepareRunner(
      String serverId,
      TxnInfo txnInfo, 
      ArrayBlockingQueue<RcTxnPrepareResult> prepareResultQueue,
      RcTxnCoordinatorRpcFacade rpcFacade) {
    this.serverId = serverId;
    this.txnInfo = txnInfo;
    this.prepareResultQueue = prepareResultQueue;
    this.rpcFacade = rpcFacade;
  }
  
  @Override
  public void run() {
    Boolean isPrepared = false;
    if (this.serverId.equals(RcServer.SERVER_ID)) {
      // Local prepare request
      isPrepared = RcServer.RC_DB.prepareTxn(
          this.txnInfo.txnId,
          this.txnInfo.getReadKeyList(), 
          this.txnInfo.getWriteKeyList(),
          this.txnInfo.getWriteValList());
    } else { 
      // Remote prepare request
      isPrepared = this.rpcFacade.prepareTxn(this.serverId, this.txnInfo);
    }
    try {
      this.prepareResultQueue.put(new RcTxnPrepareResult(this.serverId, isPrepared));
    } catch (InterruptedException e) {
      logger.error(e.getMessage());
      // TODO Solves that this exception causes the consumer to block on the queue without receiving the result.
    }
  }
  
}
