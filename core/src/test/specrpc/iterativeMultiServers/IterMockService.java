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

import java.io.IOException;
import java.util.Date;

import Waterloo.MultiSocket.exception.ConnectionCloseException;
import Waterloo.MultiSocket.exception.MultiSocketValidException;

import rpc.execption.NoClientStubException;
import specrpc.exception.SpeculationFailException;
import specrpc.server.api.SpecRpcHostObject;

public class IterMockService extends SpecRpcHostObject {

  private final String serverID;// for debug info
  private Database serverDB;

  public IterMockService(String id, Database db) {
    this.serverID = id;
    this.serverDB = db;
  }

  public String getServerID() {
    return this.serverID;
  }

  // RPC
  public Boolean putValue(String key, String value, Long calTime) {

    try {
      this.specRPCFacade.specReturn(false);// test
      this.specRPCFacade.specReturn(true);// test

      if (calTime.longValue() > 0) {
        Thread.sleep(calTime.longValue());
      }
      // Blocks until caller commit
      this.specRPCFacade.specBlock();

      long timestamp = new Date().getTime();
      // Writes into database
      this.serverDB.put(key, value, timestamp);
    } catch (NoClientStubException | InterruptedException | IOException | MultiSocketValidException
        | ConnectionCloseException e) {
      e.printStackTrace();
    } catch (SpeculationFailException e) {
      ;
    }

    return true;
  }

  public String getValue(String key, Long calTime) {
    String result = null;
    try {

      if (calTime.longValue() > 0) {
        Thread.sleep(calTime.longValue());
      }

      EntryValue entry = this.serverDB.getEntry(key); // read database
      result = entry == null ? null : entry.getValue();

    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    return result;
  }
}