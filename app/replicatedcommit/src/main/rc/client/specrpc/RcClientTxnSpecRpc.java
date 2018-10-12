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

import java.io.IOException;
import java.util.Hashtable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rc.client.RcClientLib;
import rc.client.RcClientTxn;
import rc.client.txn.ClientTxnOperation;
import rc.client.txn.ClientTxnOperation.OPERATION_TYPE;
import rc.common.RcConstants;
import rc.client.txn.ReadFailedException;
import rc.client.txn.TxnException;
import rpc.execption.MethodNotRegisteredException;
import rpc.execption.UserException;
import specrpc.client.api.SpecRpcClient;
import specrpc.client.api.SpecRpcFuture;
import specrpc.client.api.SpecRpcClientStub;
import specrpc.exception.SpeculationFailException;

public class RcClientTxnSpecRpc extends RcClientTxn {

  private static final Logger logger = LoggerFactory.getLogger(RcConstants.LOGGER_TYPE);
  
  public static final int READ_KEY_VAL_MAP_INDEX = 0;
  public static final int WRITE_KEY_VAL_MAP_INDEX = 1;
  // Uses the WRITE_KEYV_VAL_MAP_INDEX to indicate if there is a failed read that does not acquire the shared lock.
  public static final int READ_FAILED_TAG_INDEX = WRITE_KEY_VAL_MAP_INDEX;
  
  public RcClientTxnSpecRpc(String txnId) {
    super(txnId);
  }

  @Override
  protected void doExecuteTxnOperations(ClientTxnOperation[] txnOpList, int startIndex)
      throws ReadFailedException, TxnException {
    if (txnOpList == null) {
      return;
    }
    
    int i = startIndex;
    for (; i < txnOpList.length; i++) {
      if (txnOpList[i].opType == OPERATION_TYPE.WRITE) {
        this.write(txnOpList[i].getKey(), txnOpList[i].getVal());
      } else {
        break;
      }
    }
    
    int firstReadIndex = i;// First read index
    if (firstReadIndex < txnOpList.length) {
      try {
        ClientTxnOperation readOp = txnOpList[firstReadIndex];
        SpecRpcClientStub readProxyStub = SpecRpcClient.bind(
            RcClientLib.CLIENT_LIB_ID,
            RcClientReadProxyService.RPC_QUORUM_READ);
        ReadCallbackFactory readCallbackFactory = new ReadCallbackFactory(
            this.txnId,
            readOp.getKey(),
            txnOpList,
            firstReadIndex + 1,
            this.readKeyValTable,
            this.writeKeyValTable);
        
        //logger.debug("Txn id= " + this.txnId + " issues read for key= " + readOp.getKey());
        
        // Performs read
        SpecRpcFuture executionResult = readProxyStub.call(null, readCallbackFactory, this.txnId, readOp.getKey());
        Hashtable<String, String>[] txnReadWriteResult = (Hashtable<String, String>[]) executionResult.getResult();
        
        //logger.debug("Txn id= " + this.txnId + " finishes read for key= " + readOp.getKey());
        
        // Records read keys for txn abort
        this.readKeyValTable = txnReadWriteResult[READ_KEY_VAL_MAP_INDEX];
        
        if (txnReadWriteResult[READ_FAILED_TAG_INDEX] == null) {
          throw new ReadFailedException("A read fails to acquire the shared lock.");
        }
        
        // Records write keys/values for txn commit
        this.writeKeyValTable = txnReadWriteResult[WRITE_KEY_VAL_MAP_INDEX];
        
        if (logger.isDebugEnabled()) {
          this.debugTxnExecResult();
        }
        
      } catch (MethodNotRegisteredException | IOException e) {
        logger.error(e.getMessage());
        e.printStackTrace();
        System.exit(RcConstants.RUNTIME_FATAL_ERROR_CODE);
      } catch (SpeculationFailException e) {
        if (logger.isDebugEnabled()) {
          logger.debug(e.getMessage());
        }
        ; // Does nothing
      } catch (InterruptedException | UserException e) {
        logger.error(e.getMessage());
        e.printStackTrace();
        // Exception happens for this txn
        throw new TxnException(e.getMessage());
      } 
    }
  }

}
