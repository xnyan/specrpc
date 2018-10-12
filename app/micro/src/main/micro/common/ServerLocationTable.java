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

package micro.common;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Properties;
import java.util.Set;

import specrpc.common.Location;
import specrpc.common.ServerLocationDirectory;

/**
 * Reuses SpecRPC's RPC signature file to locate RPC servers' ip addresses and ports
 */
public class ServerLocationTable {

  private final Hashtable<String, Location> serverLocationTable;
  
  public ServerLocationTable(String rpcSignaturesFile) throws IOException {
    this.serverLocationTable = new Hashtable<String, Location>();
    
    // Reuses SpecRPC's RPC signature file to locate RPC servers' ip addresses and ports
    Properties prop = new Properties();
    prop.load(new FileInputStream(new File(rpcSignaturesFile)));
 
    Set<Object> rpcSigSet = prop.keySet();
    for (Object rpcSigObj : rpcSigSet) {
      String rpcSig = (String) rpcSigObj;
      String locationStr = prop.getProperty(rpcSig);
      Location location = new Location(locationStr);
      
      String serverId = rpcSig.split(ServerLocationDirectory.REGEX)[0];
      this.serverLocationTable.put(serverId, location);
    }
  }
  
  public Location getServerLocation(String serverId) {
    return this.serverLocationTable.get(serverId);
  }
  
  public String getServerHostname(String serverId) {
    return this.serverLocationTable.get(serverId).hostname;
  }
  
  public int getServerPort(String serverId) {
    return this.serverLocationTable.get(serverId).port;
  }
}
