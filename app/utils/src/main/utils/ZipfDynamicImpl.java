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

public class ZipfDynamicImpl extends ZipfCalculationImpl {

  // dict[i] indicates the total zipf probability of the [0, i] elements in dict
  protected double[] dict;

  public ZipfDynamicImpl(int numOfElements, double alpha) {
    super(numOfElements, alpha);
  }

  public ZipfDynamicImpl(int numOfElements, double alpha, long randomSeed) {
    super(numOfElements, alpha, randomSeed);
  }

  public ZipfDynamicImpl(int numOfElements, double alpha, double[] dict) {
    super(numOfElements, alpha);
    this.dict = dict;
  }

  protected void init() {
    super.init();
    this.dict = new double[numOfElements + 1]; // index zero is useless
    dict[0] = 0.0d;
    for (int i = 1; i < this.dict.length; i++) {
      dict[i] = dict[i - 1] + this.getProbability(i);
    }
  }

  protected int random() {
    double rnd_prob = this.rnd.nextDouble();
    for (int i = 1; i < this.dict.length; i++) {
      if (rnd_prob < this.dict[i]) {
        return i;
      }
    }
    return this.dict.length;
  }

  public double[] getDict() {
    return this.dict;
  }

}
