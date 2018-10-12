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

import org.junit.Assert;
import org.junit.Test;

public class ByteSizeTest {

  @Test
  public void test() {
    ByteSize res = ByteSize.parseByteSize("1b");
    Assert.assertEquals(1L, res.getByteSize());
    Assert.assertEquals(ByteSize.Unit.B, res.unit);

    res = ByteSize.parseByteSize("1kb");
    Assert.assertEquals(1024L, res.getByteSize());
    Assert.assertEquals(ByteSize.Unit.K, res.unit);

    res = ByteSize.parseByteSize("1mb");
    Assert.assertEquals(1024 * 1024L, res.getByteSize());
    Assert.assertEquals(ByteSize.Unit.M, res.unit);

    res = ByteSize.parseByteSize("2gb");
    Assert.assertEquals(1024 * 1024 * 1024 * 2L, res.getByteSize());
    Assert.assertEquals(ByteSize.Unit.G, res.unit);
  }

}
