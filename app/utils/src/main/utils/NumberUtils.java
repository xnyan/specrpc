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

public class NumberUtils {

  /**
   * Count the number of digits of an integer
   * 
   * @param an
   *          integer number
   * @return the number of digits of the given number
   */
  public static int numOfDigits(int number) {
    int count = 1; // at least 1 digit
    while ((number = number / 10) != 0) {
      count++;
    }
    return count;
  }

  /**
   * Convert a non-negative integer to a fixed-length String. Use 0s to occupy the
   * spaces that the integer is not long enough If the number of digits in the
   * integer is longer than the required length, the digits in high order will be
   * omitted
   * 
   * @param number,
   *          an integer
   * @param length,
   *          the length of the String that will be returned
   * @return a String represents 'length' digits (from low order) for the number.
   */
  public static String toString(int number, int length) {
    String str = "";
    int remains = number % 10;
    number = number / 10;
    while (length > 0) {
      str = remains + str;
      length--;
      if (number == 0) {
        break;
      }
      remains = number % 10;
      number = number / 10;
    }
    while (length > 0) {
      str = "0" + str;
      length--;
    }
    return str;
  }
}
