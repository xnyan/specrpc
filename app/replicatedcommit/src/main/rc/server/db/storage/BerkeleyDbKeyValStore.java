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

package rc.server.db.storage;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.time.Duration;
import java.time.format.DateTimeParseException;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sleepycat.je.CheckpointConfig;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.je.LockTimeoutException;
import com.sleepycat.persist.EntityStore;
import com.sleepycat.persist.StoreConfig;

import rc.common.ByteSize;
import rc.common.FormatException;
import rc.common.RcConstants;

public class BerkeleyDbKeyValStore extends KeyValStore {

  private static final Logger logger = LoggerFactory.getLogger(RcConstants.LOGGER_TYPE);

  private Environment dbEnv;
  private EntityStore dbStore;
  
  public BerkeleyDbKeyValStore(Properties config) {
    super(config);
    
    // Initializes BerkeleyDB environment
    String envDir = config.getProperty(RcConstants.DB_STORAGE_BDB_ENV_HOME_PROPERTY);
    if (envDir == null) {
      logger.error("Undefined BerkeleyDB environment.");
      System.exit(RcConstants.INIT_FAIL_ERROR_CODE);
    }
    
    File dbEnvHome = new File(envDir);
    if (! dbEnvHome.exists()) {
      dbEnvHome.mkdirs();
    }
    
    EnvironmentConfig dbEnvConfig = new EnvironmentConfig();
    StoreConfig dbStoreConfig = new StoreConfig();

    Boolean isReadOnly = Boolean.parseBoolean(config.getProperty(
        RcConstants.DB_STORAGE_BDB_READ_ONLY_PROPERTY,
        RcConstants.DEFAULT_DB_STORAGE_BDB_READ_ONLY));
    // Configures the environment for read-only or not.
    dbEnvConfig.setReadOnly(isReadOnly);
    dbStoreConfig.setReadOnly(isReadOnly);

    // When the environment is opened for write, configures whether to create the
    // environment if it does not exist.
    dbEnvConfig.setAllowCreate(!isReadOnly);
    dbStoreConfig.setAllowCreate(!isReadOnly);

    Boolean isSupportTxn = Boolean.parseBoolean(config.getProperty(
        RcConstants.DB_STORAGE_BDB_TXN_PROPERTY,
        RcConstants.DEFAULT_DB_STORAGE_BDB_TXN));
    // Enables or disables transaction
    dbEnvConfig.setTransactional(isSupportTxn);
    dbStoreConfig.setTransactional(isSupportTxn);

    // Sets lock timeout in both non-transaction and transaction mode
    String lockTimeout = config.getProperty(
        RcConstants.DB_STORAGE_BDB_LOCK_TIMEOUT_PROPERTY,
        RcConstants.DEFAULT_DB_STORAGE_BDB_LOCK_TIMEOUT);
    Duration timeDuration = null;
    try {
      timeDuration = Duration.parse(lockTimeout);
    } catch (DateTimeParseException e) {
      logger.error(e.getMessage());
      System.exit(RcConstants.INIT_FAIL_ERROR_CODE);
    }
    dbEnvConfig.setLockTimeout(timeDuration.getSeconds(), TimeUnit.SECONDS);

    // Sets cache size, i.e. memory pool, which must be at least the size of working set pluses some extra size 
    ByteSize cacheByteSize = null;
    try {
      cacheByteSize = ByteSize.parseByteSize(config.getProperty(
          RcConstants.DB_STORAGE_BDB_CACHE_SIZE_PROPERTY,
          RcConstants.DEFAULT_DB_STORAGE_BDB_CACHE_SIZE));
    } catch (FormatException e) {
      logger.error(e.getMessage());
      System.exit(RcConstants.INIT_FAIL_ERROR_CODE);
    }
    long cacheSize = cacheByteSize.getByteSize();
    if (cacheSize > 0) {
      dbEnvConfig.setCacheSize(cacheSize);// in Bytes
    }
    

    this.dbEnv = new Environment(dbEnvHome, dbEnvConfig);
    logger.debug("BerkeleyDB cache size " + dbEnv.getConfig().getCacheSize() / 1024 + " KB");
    
    this.dbStore = new EntityStore(dbEnv, "BerkeleyDBKeyValStore", dbStoreConfig);
    
  }
  
  @Override
  public DataValue read(String key) {
    BerkeleyDbEntityAccessor getDBA = new BerkeleyDbEntityAccessor(this.dbStore);
    BerkeleyDbEntity entity = null;

    try {
      entity = getDBA.pimaryIdx.get(key);
    } catch (LockTimeoutException e) {
      // To avoid that the thrown exception leads to unexpected behavior,
      // catch the exception and null will be returned
      logger.error("Get timeouts, probably caused by some kind of deadlock as the timeout is set as 5 mins");
      e.printStackTrace();
      return null;
    } catch (DatabaseException e) {
      e.printStackTrace();
      return null;
    }

    if (entity != null) {
      return new DataValue(entity.value, entity.version);
    }

    return null;
  }

  @Override
  public Boolean write(String key, String value) {
    try {
      BerkeleyDbEntityAccessor putDBA = new BerkeleyDbEntityAccessor(this.dbStore);

      try {
        BerkeleyDbEntity entity = putDBA.pimaryIdx.get(key);
        long version = entity == null ? 0 : entity.version + 1;// Initial version is 0

        // Updates
        putDBA.pimaryIdx.put(new BerkeleyDbEntity(key, value, version));

      } catch (LockTimeoutException e) {
        logger.error("Put timeouts, probably caused by some kind of deadlock as the timeout is set as 5 mins");
        e.printStackTrace();
        return false;
      } catch (DatabaseException e) {
        e.printStackTrace();
        return false;
      }
      return true;
    } catch (Exception e) {
      e.printStackTrace();
      return false;
    }
  }

  @Override
  public Boolean loadData(String dataFile) {
    BufferedReader reader = null;
    try {
      reader = new BufferedReader(new FileReader(dataFile));
    } catch (FileNotFoundException e) {
      logger.error("Data file does not exist. File: " + dataFile);
      logger.error(e.getMessage());
      return false;
    }
    String line = null;
    try {
      while ((line = reader.readLine())!= null) {
        line = line.trim();
        int regexIndex = line.indexOf(KEY_VAL_REGEX);
        String key = line.substring(0, regexIndex);
        String val = line.substring(regexIndex + 1, line.length());
        this.write(key, val);
      }
      reader.close();
    } catch (IOException e) {
      logger.error(e.getMessage());
      return false;
    }
    
    return true;
  }

  @Override
  public Boolean dumpData(String dataFileDir, String dataFile) {
    this.sync();
    logger.warn("Dumping data is not implemented for BerkeleyDbKeyValStore. Only sync data to hard drive for now.");
    // TODO dump data to specified files.
    return true;
  }
  
  public void sync() {
    this.dbEnv.sync();
  }

  public void cleanLog() {
    if (this.dbEnv.cleanLog() > 0) {
      CheckpointConfig force = new CheckpointConfig();
      force.setForce(true);
      this.dbEnv.checkpoint(force);
    }
  }

  public boolean shutdown() {
    if (this.dbStore != null) {
      try {
        this.dbStore.close();
      } catch (DatabaseException e) {
        logger.error("Failed to close BerkeleyDB store: " + e.toString());
        return false;
      }
    }

    if (this.dbEnv != null) {
      try {
        this.dbEnv.sync();
        this.dbEnv.cleanLog();
        this.dbEnv.close();
      } catch (DatabaseException e) {
        logger.error("Failed to close BerkeleyDB environment" + e.toString());
        return false;
      }
    }
    
    logger.info("Successfully shut down BerkeleyDB Key-Val Store.");
    return true;
  }
}
