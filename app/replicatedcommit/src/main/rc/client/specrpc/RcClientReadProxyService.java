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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import Waterloo.MultiSocket.exception.ConnectionCloseException;
import Waterloo.MultiSocket.exception.MultiSocketValidException;
import rc.client.RcClientLib;
import rc.client.ReadQuorum;
import rc.common.RcConstants;
import rc.common.TxnReadResult;
import rpc.execption.NoClientStubException;
import specrpc.common.RpcSignature;
import specrpc.common.api.SpecRpcFacade;
import specrpc.exception.SpeculationFailException;
import specrpc.server.api.SpecRpcHost;

public class RcClientReadProxyService implements SpecRpcHost {
  
  private static final Logger logger = LoggerFactory.getLogger(RcConstants.LOGGER_TYPE);
  
  // RPCs
  public static final String QUORUM_READ = "quorumRead";
  public static final RpcSignature RPC_QUORUM_READ = new RpcSignature(
      RcClientReadProxyService.class.getName(), // class name
      QUORUM_READ, // method name
      TxnReadResult.class, // return type
      String.class, // txn ID
      String.class // read key
      );

  private SpecRpcFacade specRpcFacade;

  @Override
  public void bind(SpecRpcFacade specRPCFacade) {
    this.specRpcFacade = specRPCFacade;
  }

  public TxnReadResult quorumRead(String txnId, String key) {
    String[] serverIdList = RcClientLib.RC_SERVER_LOCATION_SERVICE.getServerIdList(key);
    ReadQuorum readQuorum = new ReadQuorum(RcClientLib.QUORUM_NUM);

    // Asynchronously calls RPCs to all participant servers in all DCs
    for (String serverId : serverIdList) {
      RcClientLib.THREAD_POOL.execute(new Runnable() {
        @Override
        public void run() {
          /* 
           * Does not need to continue the speculation dependency because shared locks
           * that are acquired by incorrectly speculative reads will not block concurrent
           * txns, and the shared locks should be released at prepare phase.
           */
          //logger.debug("ReadProxy: Txn id= " + txnId + " issues read for key= " + key + " to server id= " + serverId);
          TxnReadResult readResult = RcClientLib.CLIENT_RPC_FACADE.read(serverId, txnId, key);
          //logger.debug("ReadProxy: Txn id= " + txnId + " finishes read for key= " + key + " from server id= " + serverId + " result= " + readResult);

          if (readResult == null) {
            // Exception happens, treats as a failed lock.
            readQuorum.failLock();
          } else {
            if (readResult.isSharedLockAcquired) {
              readQuorum.acquireLock(readResult);
            } else {
              readQuorum.failLock();
            }
          }
          
          synchronized(readQuorum) {
            //logger.debug("ReadProxy: Txn id= " + txnId + " nofities quorum for one reply for key= " + key + " server id= " + serverId);
            readQuorum.notifyAll();
          }
        }
      });
    }

    // Waits for read response
    try {
      synchronized(readQuorum) {
        if (readQuorum.isAnyReadReturn() == false) {
          // Waits for at least one read response
          //logger.debug("ReadProxy: Txn id= " + txnId + " waits for at least one reply for key= " + key);
          readQuorum.wait();
          //logger.debug("ReadProxy: Txn id= " + txnId + " wakes up for at least one reply for key= " + key);
        }
      }
      
      if (readQuorum.isAcquireQuorumLocks() == null) {
        // If Quorum response is not achieved yet, takes the first 
        // response (the latest one if there are concurrent responses) as a prediction.
        TxnReadResult prediction = readQuorum.getTxnReadResult();
        if (prediction == null) {
          // Assumes that the read fails to acquire the lock if any exception happens
          prediction = new TxnReadResult(null, null, false);
        }
        // Sends the prediction to the client.
        this.specRpcFacade.specReturn(prediction);
        
        // Waits for a Quorum of read response  
        synchronized(readQuorum) { 
          // After grabbing the monitor, checks again to avoid that the notification happens before wait().
          while (readQuorum.isAcquireQuorumLocks() == null) {
            //logger.debug("ReadProxy: Txn id= " + txnId + " waits for quorum reply for key= " + key);
            readQuorum.wait();
            //logger.debug("ReadProxy: Txn id= " + txnId + " wakes up for a reply for key= " + key);
          }
        }
      }
    } catch (InterruptedException | NoClientStubException | IOException | MultiSocketValidException
        | ConnectionCloseException e) {
      logger.error(e.getMessage());
      e.printStackTrace();
      System.exit(RcConstants.RUNTIME_FATAL_ERROR_CODE);
      try {
        this.specRpcFacade.throwNonSpecExceptionToClient(e.getMessage());
      } catch (NoClientStubException | InterruptedException | IOException | MultiSocketValidException
          | ConnectionCloseException e1) {
        logger.error(e1.getMessage());
        e1.printStackTrace();
        System.exit(RcConstants.RUNTIME_FATAL_ERROR_CODE);
      } catch (SpeculationFailException e1) {
        ; // Does nothing
      }
    } catch (SpeculationFailException e) {
      ; // Does nothing
    }

    if (readQuorum.isAcquireQuorumLocks()) {
      // Read succeeds
      //logger.debug("ReadProxy: Txn id= " + txnId + " succeeded to read for key= " + key);
      return readQuorum.getTxnReadResult();
    } else {
      // Read fails
      //logger.debug("ReadProxy: Txn id= " + txnId + " failed to read for key= " + key);
      return new TxnReadResult(null, null, false);
    }
  }
}
