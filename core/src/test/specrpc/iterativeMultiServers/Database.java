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

package specrpc.iterativeMultiServers;

import java.util.Enumeration;
import java.util.Hashtable;

/*
 * Faked Simple Database: 
 * key-->value
 * 
 * Singleton Pattern for that each server has only one database
 */
public class Database {

  private final String serverID;

  // singleton pattern
  private static Database db = null;

  public synchronized static Database getDatabase(String serverID) {
    if (db == null) {
      db = new Database(serverID);
    }
    return db;
  }

  // properties
  private Hashtable<String, EntryValue> database;

  private Database(String id) {
    this.serverID = id;
    this.database = new Hashtable<String, EntryValue>();
  }

  public EntryValue getEntry(String key) {
    return this.database.get(key);
  }

  public String getValue(String key) {
    EntryValue entry = this.database.get(key);
    return entry == null ? null : entry.getValue();
  }

  public synchronized void put(String key, String value, long timestamp) {
    EntryValue entry = this.database.get(key);
    if (entry == null) {
      entry = new EntryValue(value);
      entry.setTimestamp(timestamp);
      this.database.put(key, entry);
    } else {
      entry.setValue(value);
      entry.setTimestamp(timestamp);
    }
  }

  // test
  public void printDB() {
    if (!(serverID == null || serverID.equals(""))) {
      System.out.println("\n----------Database of Server : " + serverID + "----------");
    }

    Enumeration<String> keys = this.database.keys();
    String key = null;
    while (keys.hasMoreElements()) {
      key = keys.nextElement();
      System.out.println(key + " --> " + this.database.get(key).toString());
    }

    System.out.println("---------------------------------------------------------------------\n");
  }

}
