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
import rc.client.txn.ClientTxnOperation;
import rc.client.txn.ClientTxnOperation.OPERATION_TYPE;
import rc.common.RcConstants;
import rc.common.TxnReadResult;
import rpc.execption.MethodNotRegisteredException;
import rpc.execption.UserException;
import specrpc.client.api.SpecRpcCallback;
import specrpc.client.api.SpecRpcFuture;
import specrpc.client.api.SpecRpcClientStub;
import specrpc.common.api.SpecRpcFacade;
import specrpc.exception.SpeculationFailException;

public class ReadCallback implements SpecRpcCallback {
  
  private static final Logger logger = LoggerFactory.getLogger(RcConstants.LOGGER_TYPE);

  private SpecRpcFacade specRpcFacade;

  public final String txnId;
  public final String readKey; // The read key of the RPC that triggers this callback.
  protected final ClientTxnOperation[] txnOpList;
  protected final int opStartIndex; // The index of the operation that should be performed
  
  protected Hashtable<String, String> readKeyValTable; // thread safe
  protected Hashtable<String, String> writeKeyValTable; // thread safe

  public ReadCallback(
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
    // Does not share the same keyVal table instance for either concurrent Callbacks for the same RPC or recursive Callbacks
    // For primitive types, such as String, the data in the map are hard copied via clone. 
    this.readKeyValTable = (Hashtable<String, String>) readKeyValTable.clone();
    this.writeKeyValTable = (Hashtable<String, String>) writeKeyValTable.clone();
  }
  
  @Override
  public void bind(SpecRpcFacade specRPCFacade) {
    this.specRpcFacade = specRPCFacade;
  }

  /**
   * When read locks are acquired, but the prediction is different from the actual
   * read value, we should release all the keys that are acquired because of the
   * prediction if the keys are not required based on the actual read value.
   * 
   * However, in ReplicatedCommit protocol, holding shared locks on unnecessary
   * read keys does not matter until commit time that will verify if still holding
   * the required shared locks. This is because exclusive locks will take over
   * shared locks. Therefore, we can buffer the unnecessary shared locks until
   * commit time. And when coordinator asks participants to prepare to commit a
   * txn, each participant only verifies the shared locks that are required and
   * releases those that are not required.
   * 
   * TODO implement the above lock releasing at prepare phase so that an
   * unnecessary read key will not hold the shared lock for a locked prepared txn,
   * which may cause a concurrent txn fails to grab the exclusive lock on the key.
   * 
   * Currently, we assume that a txn always reads the same set of keys.
   */
  @Override
  public Object run(Object rpcReturnValue) throws SpeculationFailException, InterruptedException {
    TxnReadResult readResult = (TxnReadResult) rpcReturnValue;
    if (readResult == null) {
      logger.error("Quorum-proxy txn read (RPC) should not return null. Debug!");
      System.exit(RcConstants.RUNTIME_FATAL_ERROR_CODE);
    }
    
    //logger.debug("Txn id= " + this.txnId + " gets read reply for key= " + this.readKey + ", result = " + readResult);
    
    // Return result
    Hashtable<String, String>[] returnResult = (Hashtable<String, String>[]) new Hashtable[2];
    
    if (! readResult.isSharedLockAcquired) {
      /*
       * When a read fails, there is no need to issue the following operations.
       * 
       * In speculative execution, the read may speculatively fail. Therefore, we
       * need the following check.
       * 
       * If a read fails to acquire a lock in speculation mode, we can not abort the
       * txn. We have to wait until the actual read finishes to continue. We use
       * specBlock to implement this.
       * 
       */
      this.specRpcFacade.specBlock();
      
      if (! readResult.isSharedLockAcquired) {
        // Actually fails to acquire the read lock. Stops executing the txn.
        returnResult[RcClientTxnSpecRpc.READ_KEY_VAL_MAP_INDEX] = this.readKeyValTable;
        returnResult[RcClientTxnSpecRpc.READ_FAILED_TAG_INDEX] = null;
        return returnResult;
      }
    }
    
    if (readResult.val == null) {
      this.readKeyValTable.put(this.readKey, RcConstants.READ_NULL_VALUE);
    } else {
      this.readKeyValTable.put(this.readKey, readResult.val);
    }
    
    int nextOpIndex = this.opStartIndex;
   
    // More operations to execute
    while (nextOpIndex < this.txnOpList.length) {
      ClientTxnOperation op = this.txnOpList[nextOpIndex];
      if(op.opType == OPERATION_TYPE.WRITE) {
        // Buffers all writes
        this.writeKeyValTable.put(this.txnOpList[nextOpIndex].getKey(), this.txnOpList[nextOpIndex].getVal());
        nextOpIndex++;
      } else if (op.opType == OPERATION_TYPE.READ) {
        if (this.readKeyValTable.containsKey(op.getKey())) {
          // Local read
          // Application logic goes here if any
          nextOpIndex++;
        } else {
          // Remote read
          break;
        }
      } else {
        // Undefined operation type
        logger.error("Undefined operation type = " + op.opType);
        System.exit(RcConstants.RUNTIME_FATAL_ERROR_CODE);
      }
    }
    
    boolean followingReadSuccess = true;
    
    if (nextOpIndex < this.txnOpList.length) {
      // One more remote read
      ClientTxnOperation readOp = txnOpList[nextOpIndex];
      try {
        // Continues speculation dependency
        SpecRpcClientStub readProxyStub = this.specRpcFacade.bind(
            RcClientLib.CLIENT_LIB_ID,
            RcClientReadProxyService.RPC_QUORUM_READ);
        ReadCallbackFactory readCallbackFactory = new ReadCallbackFactory(
            this.txnId,
            readOp.getKey(),//this.readKey,
            txnOpList,
            nextOpIndex + 1,
            this.readKeyValTable,
            this.writeKeyValTable);
        
        //logger.debug("Txn id= " + this.txnId + " issues read for key= " + readOp.getKey());
        SpecRpcFuture followingExecution = readProxyStub.call(null, readCallbackFactory, this.txnId, readOp.getKey());        
        Hashtable<String, String>[] followingReadWriteResult = (Hashtable<String, String>[]) followingExecution.getResult();
        //logger.debug("Txn id= " + this.txnId + " finishes read for key= " + readOp.getKey());
        
        /*
        if (this.readKeyValTable.size() != followingReadWriteResult[RcClientTxnSpecRpc.READ_KEY_VAL_MAP_INDEX].size()) {
          // Records read keys for txn abort if more reads happen in the following execution
          this.readKeyValTable = followingReadWriteResult[RcClientTxnSpecRpc.READ_KEY_VAL_MAP_INDEX];
        }
        */ 
        //this.readKeyValTable.putAll(followingReadWriteResult[RcClientTxnSpecRpc.READ_KEY_VAL_MAP_INDEX]); 
        // The following Callback's return contains the read result at this level
        this.readKeyValTable = followingReadWriteResult[RcClientTxnSpecRpc.READ_KEY_VAL_MAP_INDEX];
        
        if (followingReadWriteResult[RcClientTxnSpecRpc.READ_FAILED_TAG_INDEX] == null) {
          // There is one read fails to acquire the shared lock in the following execution
          followingReadSuccess = false;
        } else {
          if (this.writeKeyValTable.size() != followingReadWriteResult[RcClientTxnSpecRpc.WRITE_KEY_VAL_MAP_INDEX].size())
          // Records write keys/values for txn commit
          this.writeKeyValTable = followingReadWriteResult[RcClientTxnSpecRpc.WRITE_KEY_VAL_MAP_INDEX];
        }
      } catch (MethodNotRegisteredException | IOException e) {
        logger.error(e.getMessage());
        e.printStackTrace();
        System.exit(RcConstants.RUNTIME_FATAL_ERROR_CODE);
      } catch (UserException e) {
        logger.error("User exception.");
        logger.error(e.getMessage());
        //TODO handles user-defined exception if any
      }     
    }

    returnResult[RcClientTxnSpecRpc.READ_KEY_VAL_MAP_INDEX] = this.readKeyValTable;

    if (followingReadSuccess) {
      returnResult[RcClientTxnSpecRpc.WRITE_KEY_VAL_MAP_INDEX] = this.writeKeyValTable;
    } else {
      returnResult[RcClientTxnSpecRpc.READ_FAILED_TAG_INDEX] = null;
    }

    return returnResult;
  }
  
}
