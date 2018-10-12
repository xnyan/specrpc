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

import org.apache.commons.math3.distribution.ZipfDistribution;

public class ZipfDistributionImpl extends ZipfGenerator {

  private ZipfDistribution zipfDistribution;

  public ZipfDistributionImpl(int numOfElements, double exponent) {
    super(numOfElements, exponent);
  }

  protected void init() {
    this.zipfDistribution = new ZipfDistribution(this.numOfElements, this.alpha);
  }

  @Override
  public int random() {
    return this.zipfDistribution.sample();

    /*
     * // slow implementation int rank = this.rnd.nextInt(this.numOfElements) + 1;
     * double frequency = this.zipfDistribution.probability(rank); double dice =
     * this.rnd.nextDouble();
     * 
     * while(dice > frequency) { rank = this.rnd.nextInt(this.numOfElements) + 1;
     * frequency = this.zipfDistribution.probability(rank); dice =
     * this.rnd.nextDouble(); }
     * 
     * return rank;
     */
  }

  @Override
  public double getProbability(int num) {
    return this.zipfDistribution.probability(num);
  }
}
