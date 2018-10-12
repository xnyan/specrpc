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

package rc.server.db.log;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rc.common.RcConstants;
import rc.common.TxnInfo;
import rc.server.db.Txn;

public class AsyncTxnLogger implements TxnLogger {

  private static final Logger logger = LoggerFactory.getLogger(RcConstants.LOGGER_TYPE);

  private class LogFileWriter implements Runnable {

    private BufferedWriter logBuffer;
    private BlockingQueue<Txn> txnQueue; // Txns to be logged
    private boolean isToTerminate;

    public LogFileWriter(FileWriter logFileWriter, BlockingQueue<Txn> txnBuffer) {
      this.logBuffer = new BufferedWriter(logFileWriter);
      this.txnQueue = txnBuffer;
      this.isToTerminate = false;
    }

    @Override
    public void run() {
      
      if (logger.isDebugEnabled()) {
        logger.debug("Txn logger starts.");
      }
      
      while (!this.isToTerminate) {
        Txn txn;
        try {
          txn = this.txnQueue.take();
          this.logBuffer.write(txn.createLogInfo() + "\n");
          
          if (logger.isDebugEnabled()) {
            // In debug mode, flushes to hard drive very time there is a log entry.
            this.logBuffer.flush();
            logger.debug("Txn log:\n" + txn.createLogInfo());
          }
          
        } catch (InterruptedException | IOException e) {
          logger.error(e.getMessage());
        }
      }
      try {
        this.logBuffer.close();
      } catch (IOException e) {
        logger.error(e.getMessage());
      }
    }

    public void close() {
      this.isToTerminate = true;
    }
  }

  private BlockingQueue<Txn> txnLogBuffer; // Txns to be logged
  private LogFileWriter logFileWriter; // Log file
  private Thread logFileWriterThread; // Actual logging thread

  public AsyncTxnLogger(Properties config) {
    
    // Txn log initialization 
    String logFile = config.getProperty(RcConstants.DB_TXN_LOG_FILE_PROPERTY);
    if (logFile == null) {
      String logFileDir = config.getProperty(
          RcConstants.DB_TXN_LOG_DIR_PROPERTY,
          RcConstants.DEFAULT_DB_TXN_LOG_DIR);
      String logFilePrefix = config.getProperty(
          RcConstants.DB_TXN_LOG_FILE_PREFIX_PROPERTY,
          RcConstants.DEFAULT_DB_TXN_LOG_FILE_PREFIX);
      String logFileSuffix = config.getProperty(
          RcConstants.DB_TXN_LOG_FILE_SUFFIX_PROPERTY,
          RcConstants.DEFAULT_DB_TXN_LOG_FILE_SUFFIX);
      String serverId = config.getProperty(RcConstants.SERVER_ID_PROPERTY);
      logFile = logFileDir + "/" + logFilePrefix + serverId + logFileSuffix;
    }
    Boolean isLogFileAppend = Boolean.parseBoolean(config.getProperty(
        RcConstants.DB_TXN_LOG_FILE_APPEND_PROPERTY,
        RcConstants.DEFAULT_DB_TXN_LOG_FILE_APPEND));
    
    this.txnLogBuffer = new LinkedBlockingQueue<Txn>();
    FileWriter fileWriter = null;
    try {
      fileWriter = new FileWriter(logFile, isLogFileAppend); // Opens the log file
    } catch (IOException e) {
      logger.error("Failed to open or create log file: " + logFile);
      logger.error(e.getMessage());
      System.exit(RcConstants.INIT_FAIL_ERROR_CODE);
    }
    this.logFileWriter = new LogFileWriter(fileWriter, this.txnLogBuffer);
    this.logFileWriterThread = new Thread(this.logFileWriter);
    this.logFileWriterThread.start();
  }

  @Override
  public boolean logPreparedTxn(String txnId, String[] readKeyList, String[] writeKeyList, String[] writeValList) {
    /**
     * Currently, creates a new txn instance to avoid synchronization on the txn object's txn status.
     * 
     * For example, before a PREPARED txn is actually logged, it becomes COMMITTED. The actual logging
     * thread will miss the txn's PREPARED status if there is no synchronization on the txn status.
     * Since prepared txns release their shared locks, the serialization order is the order of PREPARED
     * txns instead of their actual commit order.
     * 
     * Therefore, we can not miss txns' PREPARED statuses in the log. If prepared txns do not release 
     * their shared locks, missing PREPARED statuses may be OK as committed txns must be prepared before.
     * 
     * Using synchronization on the txn status has to block a txn's commit process until the txn's PREPARED
     * status is logged, which may be a bottleneck.
     */
    Txn txn = new Txn(txnId, readKeyList, writeKeyList, writeValList, TxnInfo.TXN_STATUS.PREPARED);
    return this.doLogTxn(txn);
  }

  @Override
  public boolean logCommittedTxn(String txnId, String[] writeKeyList, String[] writeValList) {
    Txn txn = new Txn(txnId, null, writeKeyList, writeValList, TxnInfo.TXN_STATUS.COMMITTED);
    return this.doLogTxn(txn);
  }

  @Override
  public boolean logAbortedTxn(String txnId) {
    Txn txn = new Txn(txnId, null, null, null, TxnInfo.TXN_STATUS.ABORTED);
    return this.doLogTxn(txn);
  }
  
  private boolean doLogTxn(Txn txn) {
    try {
      this.txnLogBuffer.put(txn);
    } catch (InterruptedException e) {
      logger.error(e.getMessage());
      return false;
    }
    return true;
  }
  
  public void shutdown() {
    this.logFileWriter.close();
    try {
      this.logFileWriterThread.join();
    } catch (InterruptedException e) {
      logger.error(e.getMessage());
    }
  }

}
