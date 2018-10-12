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

package rc.common;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Utils {

  private static final Logger logger = LoggerFactory.getLogger(RcConstants.LOGGER_TYPE);

  /**
   * Returns a string that shows every element in the given list.
   * 
   * @param objList
   * @return String
   */
  public static String toString(Object[] objList) {
    if (objList == null) {
      return null;
    }
    String res = "[";
    for (Object obj : objList) {
      res += obj == null ? obj : obj.toString() + ",";
    }
    // TODO removes the last "," in the returned result.
    res += "]";
    return res;
  }

  /**
   * Parses a key file to read in the keys as a list.
   * @param keyFile
   * @return a list of keys
   */
  public static String[] readKeys(String keyFile) {
    String[] keyList = null;
    // Parses key file
    try {
      BufferedReader keyReader = new BufferedReader(new FileReader(keyFile));
      int keyNum = Integer.parseInt(keyReader.readLine());
      keyList = new String[keyNum];
      for (int i = 0; i < keyNum; i++) {
        keyList[i] = keyReader.readLine();
      }
      keyReader.close();
    } catch (NumberFormatException | IOException e) {
      logger.error(e.getMessage());
      e.printStackTrace();
      System.exit(RcConstants.INIT_FAIL_ERROR_CODE);
    }
    return keyList;
  }
}
