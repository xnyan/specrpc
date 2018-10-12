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

package utils;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Properties;

import utils.sorting.Sorting;

public class ConfigurationHelper {

  /*
   * File Format serverId_1 otherProperties serverId_2 otherProperties ...
   */
  public static ArrayList<String> loadServerIds(String file) throws IOException {
    ArrayList<String> list = new ArrayList<String>();
    FileInputStream serversInput = new FileInputStream(file);
    Properties serverSet = new Properties();
    serverSet.load(serversInput);
    serversInput.close();

    Enumeration<Object> serverIdSet = serverSet.keys();
    while (serverIdSet.hasMoreElements()) {
      list.add((String) serverIdSet.nextElement());
    }
    // sort to guarantee that each server sees the same order of server ids
    list = Sorting.sort(list);
    return list;
  }
}
