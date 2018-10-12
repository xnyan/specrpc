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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Not thread safe.
 */
public class RcServerLocationService {

  private static final Logger logger = LoggerFactory.getLogger(RcConstants.LOGGER_TYPE);

  public final int dcNum;
  public final String[] dcIdList;
  public final int shardNum; // number of shards (i.e. servers)
  public final String[] shardIdList;
  // A server id is "dcId" + SERVER_ID_REGEX + "shardId"
  public final HashMap<String, HashMap<String, String>> shardIdServerIdMap; // shardId --> <dcId --> serverId>
  public final HashMap<String, String[]> shardIdServerIdListMap; // shardId --> all server IDs in all DCs

  public RcServerLocationService(Properties config) {
    int dcNum = 0, shardNum = 0;
    try {
      dcNum = Integer.parseInt(config.getProperty(RcConstants.DC_NUM_PROPERTY));
      shardNum = Integer.parseInt(config.getProperty(RcConstants.DC_SHARD_NUM_PROPERTY));
    } catch (NumberFormatException e) {
      logger.error(e.getMessage());
      System.exit(RcConstants.INIT_FAIL_ERROR_CODE);
    }

    String dcIdList = config.getProperty(RcConstants.DC_ID_LIST_PROPERTY);
    String shardIdList = config.getProperty(RcConstants.DC_SHARD_ID_LIST_PROPERTY);
    if (dcIdList == null || dcIdList.length() == 0) {
      logger.error("Undefined DC id list.");
      System.exit(RcConstants.INIT_FAIL_ERROR_CODE);
    }

    if (shardIdList == null || shardIdList.length() == 0) {
      logger.error("Undefined DC id list.");
      System.exit(RcConstants.INIT_FAIL_ERROR_CODE);
    }

    this.dcNum = dcNum;
    this.shardNum = shardNum;
    this.dcIdList = dcIdList.split(RcConstants.ID_REGEX);
    // TODO: make sure each server has the same order of ids in the list in case
    // that servers have different
    // configuration files that may have different order of ids in the list
    // property.
    this.shardIdList = shardIdList.split(RcConstants.ID_REGEX);

    if (this.dcIdList.length != this.dcNum) {
      logger.error("The number of DC ids does not match the number of DCs.");
      System.exit(RcConstants.INIT_FAIL_ERROR_CODE);
    }

    if (this.shardIdList.length != this.shardNum) {
      logger.error("The number of server ids does not match the number of shards (i.e. servers)");
      System.exit(RcConstants.INIT_FAIL_ERROR_CODE);
    }

    this.shardIdServerIdMap = new HashMap<String, HashMap<String, String>>();
    this.shardIdServerIdListMap = new HashMap<String, String[]>();
    this.buildKeyServerIdMap();
  }

  private void buildKeyServerIdMap() {
    for (int i = 0; i < this.shardIdList.length; i++) {
      String[] serverIdList = new String[this.dcIdList.length];
      HashMap<String, String> dcServerIdMap = new HashMap<String, String>();
      for (int j = 0; j < this.dcIdList.length; j++) {
        serverIdList[j] = this.dcIdList[j] + RcConstants.SERVER_ID_REGEX + shardIdList[i];
        dcServerIdMap.put(this.dcIdList[j], serverIdList[j]);
      }
      this.shardIdServerIdListMap.put(shardIdList[i], serverIdList);
      this.shardIdServerIdMap.put(shardIdList[i], dcServerIdMap);
    }
  }

  /**
   * Returns the list of server IDs in all DCs, where each sever has the key's
   * data shard in a DC.
   * 
   * @param key
   * @return a list of server IDs
   */
  public String[] getServerIdList(String key) {
    return this.shardIdServerIdListMap.get(this.shardIdList[this.getShardIndex(key)]);
  }

  /**
   * Returns the server ID, where the server has the key's data shard in the given
   * DC.
   * 
   * @param key
   * @param dcId
   * @return the server ID in the given DC
   */
  public String getServerId(String key, String dcId) {
    return this.shardIdServerIdMap.get(this.shardIdList[this.getShardIndex(key)]).get(dcId);
  }
  
  /**
   * Returns all the server IDs in all DCs for the specified shard id.
   * 
   * @param dcId
   * @param shardId
   * @return
   */
  public String[] getCoordinatorServerIdList(String shardId) {
    return this.shardIdServerIdListMap.get(shardId);
  }
  
  /**
   * Returns the shard ID that the key is mapped to
   * @param key
   * @return Shard ID, String
   */
  public String getShardId(String key) {
    return this.shardIdList[this.getShardIndex(key)];
  }

  /**
   * Returns the index of the shard ID in the shard ID list, where
   * the shard is the one that the key is mapped to. 
   * @param key
   * @return the index of the shard ID int the shard ID list
   */
  private int getShardIndex(String key) {
    int num = key.hashCode();
    num = Math.abs(num);
    if (num < 0)
      return 0; // When num is Integer.MIN_VALUE
    return num % this.shardNum;
  }
  
  /**
   * Dispatch a txn's read/write keys to corresponding shard servers in the same
   * DC.
   * 
   * @param txnId
   * @param readKeyList
   * @param writeKeyList
   * @param writeValList
   * @return A mapping from server ID to the txn information that includes the
   *         read/write keys/values.
   */
  public HashMap<String, TxnInfo> mapTxnToShardServers(String dcId, String txnId, String[] readKeyList,
      String[] writeKeyList, String[] writeValList) {
    HashMap<String, TxnInfo> serverIdTxnInfoMap = new HashMap<String, TxnInfo>();
    if (readKeyList != null && readKeyList.length != 0) {
      for (String readKey : readKeyList) {
        String serverId = this.getServerId(readKey, dcId);
        if (!serverIdTxnInfoMap.containsKey(serverId)) {
          serverIdTxnInfoMap.put(serverId, new TxnInfo(txnId));
        }
        serverIdTxnInfoMap.get(serverId).addReadKey(readKey);
      }
    }

    if (writeKeyList != null && writeKeyList.length != 0) {
      for (int i = 0; i < writeKeyList.length; i++) {
        String serverId = this.getServerId(writeKeyList[i], dcId);
        if (!serverIdTxnInfoMap.containsKey(serverId)) {
          serverIdTxnInfoMap.put(serverId, new TxnInfo(txnId));
        }
        serverIdTxnInfoMap.get(serverId).addWriteKeyVal(writeKeyList[i], writeValList[i]);
      }
    }

    return serverIdTxnInfoMap;
  }
  
  /**
   * Maps a list of keys to all shard servers in all DCs. Returns a map, where a
   * mapping entry is a map from a server to a list of keys.
   * 
   * @param readKeyList
   * @return A map from server ID to a list of keys on the server.
   */
  public HashMap<String, ArrayList<String>> mapReadKeysToShardServers(String[] readKeyList) {
    HashMap<String, ArrayList<String>> map = new HashMap<String, ArrayList<String>>();
    if (readKeyList != null) {
      for (String readKey : readKeyList) {
        String[] serverIdList = this.getServerIdList(readKey);
        if (! map.containsKey(serverIdList[0])) {
          for (String serverId : serverIdList) {
            map.put(serverId, new ArrayList<String>());
          }
        }
        for (String serverId : serverIdList) {
          map.get(serverId).add(readKey);
        }
      }
      
    }
    return map;
  }
}
