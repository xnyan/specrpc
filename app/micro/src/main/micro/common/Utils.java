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

import java.util.Random;

public class Utils {

  public static int strLength = 64;
  public static char[] strSet = {'a'};
  
  public static String genString(Random rnd, int length) {
    String ret = "";
    for (int i = 0; i < Utils.strLength; i++) {
      ret += strSet[rnd.nextInt(strSet.length)];
    }
    return ret;
  }
}
