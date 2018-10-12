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

public class ZipfDynamicBinarySearchImpl extends ZipfDynamicImpl {

  public ZipfDynamicBinarySearchImpl(int numOfElements, double alpha) {
    super(numOfElements, alpha);
  }

  public ZipfDynamicBinarySearchImpl(int numOfElements, double alpha, long randomSeed) {
    super(numOfElements, alpha, randomSeed);
  }

  public ZipfDynamicBinarySearchImpl(int numOfElements, double alpha, double[] dict) {
    super(numOfElements, alpha, dict);
  }

  protected int random() {
    double rnd_prob = this.rnd.nextDouble();
    return this.search(rnd_prob, 1, this.dict.length - 1);
  }

  /**
   * return the index of prob that falls in [dict[index -1], dict[index]) return
   * -1 if not found
   */
  private int search(double prob, int startIndex, int endIndex) {
    if (endIndex < startIndex)
      return this.dict.length;
    int mid = (startIndex + endIndex) / 2;
    if (dict[mid - 1] <= prob && prob < dict[mid])
      return mid;
    else if (prob < dict[mid - 1])
      return search(prob, startIndex, mid - 1);
    else // (prob > dict[mid])
      return search(prob, mid + 1, endIndex);
  }

}
