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

import java.util.Properties;

public abstract class KeyValStore {
  
  public static final String KEY_VAL_REGEX = "=";
  
  protected Properties config;
  
  public KeyValStore(Properties config) {
    this.config = config;
  }

  /**
   * Return the value with the given key. Return null if key does not exist.
   * 
   * @param key
   * @return value
   */
  public abstract DataValue read(String key);

  /**
   * Update data with the given key-value pair and its version
   * 
   * @param key
   * @param value
   * @param version
   * @return true if writing data is successfully, otherwise false.
   */
  public abstract Boolean write(String key, String value);
  
  /**
   * Load data from the given data file.
   * 
   * Data File Format
   * key1=value1
   * key2=value2
   * ...
   *
   * @param dataFile
   * @return true if successful, otherwise false.
   */
  public abstract Boolean loadData(String dataFile);
  
  /**
   * Dump data into hard drives with given data file directory or data file
   * 
   * @param dataFile
   * @return true if successful, otherwise false
   */
  public abstract Boolean dumpData(String dataFileDir, String dataFile);
  
  /**
   * Flushes in-memory data to hard drive if any
   */
  public abstract void sync();
  
  /**
   * Cleans up data log if any
   */
  public abstract void cleanLog();
  
  /**
   * Shuts down storage environment.
   * 
   * @return true if successful, otherwise false.
   */
  public abstract boolean shutdown();
}
