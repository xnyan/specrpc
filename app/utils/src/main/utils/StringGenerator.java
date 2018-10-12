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

import java.util.Random;

public class StringGenerator {

  // a fixed-length string with random chars
  private static Random strRnd = new Random(System.nanoTime());
  private final static char[] chars = "abcdefghijklmnopqrstuvwxyz".toCharArray();

  public static String randomString(final int length) {
    StringBuilder str = new StringBuilder(length);
    for (int i = 0; i < length; i++) {
      str.append(chars[strRnd.nextInt(chars.length)]);
    }
    return str.toString();
  }

  // a string with specified number of patterns
  public static String fixedString(String pattern, int num) {
    String str = "";
    for (int i = 0; i < num; i++) {
      str += pattern;
    }
    return str;
  }
}
