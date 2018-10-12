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

import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;

import org.junit.Assert;
import org.junit.Test;

public class TxnReadResultTest {

  @Test
  public void testEquivalence() {
    // Equivalence on isAcquiredLock
    TxnReadResult readResult = new TxnReadResult(null, null, false);
    Assert.assertEquals(new TxnReadResult(null, null, false), readResult);
    
    readResult = new TxnReadResult(null, null, true);
    assertThat(readResult, not(equalTo(new TxnReadResult(null, null, false))));
    
    // Equivalence on version
    readResult = new TxnReadResult(null, 5l, false);
    Assert.assertEquals(new TxnReadResult(null, 5l, false), readResult);
    
    readResult = new TxnReadResult(null, 5l, true);
    assertThat(readResult, not(equalTo(new TxnReadResult(null, null, true))));
    
    readResult = new TxnReadResult(null, null, true);
    assertThat(readResult, not(equalTo(new TxnReadResult(null, 5l, true))));
    
    readResult = new TxnReadResult(null, 5l, true);
    assertThat(readResult, not(equalTo(new TxnReadResult(null, 4l, true))));
    
    // Equivalence on value
    readResult = new TxnReadResult("a", 5l, false);
    Assert.assertEquals(new TxnReadResult("a", 5l, false), readResult);
    
    readResult = new TxnReadResult("a", 5l, true);
    Assert.assertEquals(new TxnReadResult("a", 5l, true), readResult);
    
    readResult = new TxnReadResult("a", 5l, true);
    assertThat(readResult, not(equalTo(new TxnReadResult(null, 5l, true))));
    
    readResult = new TxnReadResult(null, 5l, true);
    assertThat(readResult, not(equalTo(new TxnReadResult("a", 5l, true))));
    
    readResult = new TxnReadResult("a", 5l, true);
    assertThat(readResult, not(equalTo(new TxnReadResult("b", 5l, true))));
  }

}
