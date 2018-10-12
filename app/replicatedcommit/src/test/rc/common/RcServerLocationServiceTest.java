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

import java.util.Properties;

import org.junit.Assert;
import org.junit.Test;

public class RcServerLocationServiceTest {

  @Test
  public void test() {
    Properties config = new Properties();
    config.setProperty(RcConstants.DC_NUM_PROPERTY, "3");
    config.setProperty(RcConstants.DC_ID_LIST_PROPERTY, "1,2,3");
    config.setProperty(RcConstants.DC_SHARD_NUM_PROPERTY, "4");
    config.setProperty(RcConstants.DC_SHARD_ID_LIST_PROPERTY, "a,b,c,d");

    RcServerLocationService locationService = new RcServerLocationService(config);

    Assert.assertEquals(3, locationService.dcNum);
    String[] dcIdList = { "1", "2", "3" };
    Assert.assertArrayEquals(dcIdList, locationService.dcIdList);

    Assert.assertEquals(4, locationService.shardNum);
    String[] shardIdList = { "a", "b", "c", "d" };
    Assert.assertArrayEquals(shardIdList, locationService.shardIdList);

    String[] serverIdListOne = { "1-a", "2-a", "3-a" };
    Assert.assertArrayEquals(serverIdListOne, locationService.shardIdServerIdListMap.get("a"));

    String[] serverIdListTow = { "1-b", "2-b", "3-b" };
    Assert.assertArrayEquals(serverIdListTow, locationService.shardIdServerIdListMap.get("b"));

    String[] serverIdListThree = { "1-c", "2-c", "3-c" };
    Assert.assertArrayEquals(serverIdListThree, locationService.shardIdServerIdListMap.get("c"));

    String[] serverIdListFour = { "1-d", "2-d", "3-d" };
    Assert.assertArrayEquals(serverIdListFour, locationService.shardIdServerIdListMap.get("d"));

    Assert.assertEquals("1-a", locationService.shardIdServerIdMap.get("a").get("1"));
    Assert.assertEquals("2-a", locationService.shardIdServerIdMap.get("a").get("2"));
    Assert.assertEquals("1-b", locationService.shardIdServerIdMap.get("b").get("1"));
    Assert.assertEquals("3-d", locationService.shardIdServerIdMap.get("d").get("3"));

    Assert.assertEquals("1-", locationService.getServerId("0", "1").substring(0, 2));
    Assert.assertEquals("3-", locationService.getServerId("1", "3").substring(0, 2));
    Assert.assertNotNull(locationService.getServerId("abc", "3"));
    Assert.assertNull(locationService.getServerId("abc", "4"));

    // TODO test the results of getServerIdList(). The result depends on hashing.
    // String[] serverIdListFive = {"1-a", "2-a", "3-a"};
    // Assert.assertArrayEquals(serverIdListFive, map.getServerIdList("0"));
    Assert.assertEquals(3, locationService.getServerIdList("1").length);
    Assert.assertEquals(3, locationService.getServerIdList("2").length);
    Assert.assertNotNull(locationService.getServerIdList("abc"));
    Assert.assertNotNull(locationService.getServerIdList("123456"));
  }

}
