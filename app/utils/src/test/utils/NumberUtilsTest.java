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

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class NumberUtilsTest {

  @BeforeClass
  public static void setUpBeforeClass() throws Exception {
  }

  @AfterClass
  public static void tearDownAfterClass() throws Exception {
  }

  @Before
  public void setUp() throws Exception {
  }

  @After
  public void tearDown() throws Exception {
  }

  @Test
  public void testNumOfDigits() {
    assertEquals(NumberUtils.numOfDigits(0), 1);
    assertEquals(NumberUtils.numOfDigits(1), 1);
    assertEquals(NumberUtils.numOfDigits(10), 2);
    assertEquals(NumberUtils.numOfDigits(11), 2);
    assertEquals(NumberUtils.numOfDigits(100), 3);
  }

  @Test
  public void testToString() {
    assertEquals(NumberUtils.toString(0, 0), "");
    assertEquals(NumberUtils.toString(0, 1), "0");
    assertEquals(NumberUtils.toString(0, 2), "00");
    assertEquals(NumberUtils.toString(0, 3), "000");
    assertEquals(NumberUtils.toString(10, 3), "010");
    assertEquals(NumberUtils.toString(10, 5), "00010");
    assertEquals(NumberUtils.toString(10, 1), "0");
  }

}
