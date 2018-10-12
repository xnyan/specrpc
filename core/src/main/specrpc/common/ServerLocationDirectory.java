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

package specrpc.common;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Properties;

import rpc.execption.MethodNotRegisteredException;

public class ServerLocationDirectory {

  public static final String REGEX = ";";

  private final String rpcSignatureFile;
  private final Properties prop;
  private final HashMap<String, Location> rpcSigLocationTable;

  public ServerLocationDirectory(String rpcSignaturesFile) throws FileNotFoundException, IOException {
    this.rpcSignatureFile = rpcSignaturesFile;
    prop = new Properties();
    this.rpcSigLocationTable = new HashMap<String, Location>();

    this.loadRpcSigs(this.rpcSignatureFile);
  }

  private synchronized void loadRpcSigs(String rpcSigFile) throws FileNotFoundException, IOException {
    File dirFile = new File(this.rpcSignatureFile);
    if (!dirFile.exists()) {
      dirFile.createNewFile();
    }
    prop.load(new FileInputStream(dirFile));

    for (Object key : prop.keySet()) {
      String id = (String) key;
      String locStr = prop.getProperty(id);
      Location loc = new Location(locStr);
      this.rpcSigLocationTable.put(id, loc);
    }
  }

  /**
   * Set an RPC location. Not persistent
   * 
   * @param rpcId
   * @param loc
   */
  public void setRpcSigLocation(String rpcId, Location loc) {
    this.prop.setProperty(rpcId, loc.toString());
    this.rpcSigLocationTable.put(rpcId, loc);
  }

  public HashMap<String, Location> getRpcSigLocation() {
    return this.rpcSigLocationTable;
  }

  public Location lookUp(String serverIdentity, RpcSignature signature) throws MethodNotRegisteredException {
    String identity = serverIdentity + REGEX + signature.toString();
    Location loc = this.rpcSigLocationTable.get(identity);
    if (loc == null) {
      // The specified method signature is not found
      throw new MethodNotRegisteredException(serverIdentity, signature);
    }
    return loc;
  }

  public void registerAndPersist(String serverIdentity, RpcSignature signature, Location serverLocation) {
    this.registerNotPersist(serverIdentity, signature, serverLocation);
    this.persistRpcSig();
  }

  public void registerNotPersist(String serverIdentity, RpcSignature signature, Location serverLocation) {
    String identity = serverIdentity + REGEX + signature.toString();
    prop.setProperty(identity, serverLocation.toString());
    this.rpcSigLocationTable.put(identity, serverLocation);
  }

  public void persistRpcSig() {
    try {
      FileOutputStream outputStream = new FileOutputStream(this.rpcSignatureFile);
      /*
       * Rewrite the file
       * 
       * prop.store(new FileOutputStream(this.rpcSignaturesFile), null);
       */
      // Does not rewrite the file
      prop.store(outputStream, null);
      outputStream.flush();
      outputStream.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
