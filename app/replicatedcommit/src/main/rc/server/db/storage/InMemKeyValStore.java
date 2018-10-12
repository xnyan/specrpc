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
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Map.Entry;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rc.common.RcConstants;

public class InMemKeyValStore extends KeyValStore {
  
  private static final Logger logger = LoggerFactory.getLogger(RcConstants.LOGGER_TYPE);

  // Txn management only provides isolation for concurrently conflicting txns
  //private HashMap<String, DataValue> keyValMap;// Not thread safe
  private Hashtable<String, DataValue> keyValMap;// thread safe
  
  public InMemKeyValStore(Properties config) {
    super(config);
    this.keyValMap = new Hashtable<String, DataValue>();
    // TODO initializes data according to configuration
  }
  
  @Override
  public DataValue read(String key) {
    return this.keyValMap.get(key);
  }

  @Override
  public Boolean write(String key, String value) {
    if (key == null) {
      logger.error("Write operation fails because key can not be null.");
      return false;
    }
    DataValue oldVal = this.keyValMap.get(key);
    // TODO Does this guarantee that the version number is consistent across replicas in different DCs?
    long newVersion = oldVal == null ? 0 : oldVal.version + 1;
    DataValue newVal = new DataValue(value, newVersion);
    this.keyValMap.put(key, newVal);
    return true;
  }

  // When loading data, set data version to be 0
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
        this.keyValMap.put(key, new DataValue(val, 0)); // Initial version is 0
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
    // Only uses dataFile, which requires the full path.
    // Ignores dataFileDir for now.
    // TODO: implement logics to determine dataFileDir and dataFile 
    if (dataFile == null) {
      return false;
    }
    
    BufferedWriter writer = null;
    
    try {
      writer = new BufferedWriter(new FileWriter(dataFile));
      for (Entry<String, DataValue> entry : this.keyValMap.entrySet()) {
        writer.write(entry.getKey() + KEY_VAL_REGEX + entry.getValue().val + "\n");
      }
      writer.flush();
      writer.close();
    } catch (IOException e) {
      logger.error(e.getMessage());
      return false;
    }
    
    return true;
  }

  @Override
  public void sync() {
    // TODO add a persistent layer for this in-memory storage
  }

  @Override
  public void cleanLog() {
    // No log
  }

  @Override
  public boolean shutdown() {
    return true;
  }

}
