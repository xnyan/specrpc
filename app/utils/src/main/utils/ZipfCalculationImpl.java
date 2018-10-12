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

public class ZipfCalculationImpl extends ZipfGenerator {

  protected double constant; // normalization constant

  public ZipfCalculationImpl(int numOfElements, double alpha) {
    super(numOfElements, alpha);
  }

  public ZipfCalculationImpl(int numOfElements, double alpha, long randomSeed) {
    super(numOfElements, alpha, randomSeed);
  }

  protected void init() {
    this.constant = 0.0d;
    for (int i = 1; i <= this.numOfElements; i++) {
      this.constant = this.constant + (1.0 / Math.pow(i, this.alpha));
    }
    this.constant = 1.0 / this.constant;
  }

  @Override
  protected int random() {
    // generate one uniform probability [0.0, 1.0)
    double rnd_prob = this.rnd.nextDouble();
    double sum_prob = 0.0d;
    int zipfValue = 1;
    for (; zipfValue <= this.numOfElements; zipfValue++) {
      sum_prob += this.constant / Math.pow(zipfValue, this.alpha);
      if (sum_prob >= rnd_prob) {
        break;
      }
    }
    if (zipfValue > this.numOfElements) {
      zipfValue = this.numOfElements;
    }
    return zipfValue;
  }

  @Override
  public double getProbability(int num) {
    return this.constant / Math.pow(num, this.alpha);
  }
}
