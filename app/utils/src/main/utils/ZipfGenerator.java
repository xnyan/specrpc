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

public abstract class ZipfGenerator extends NumberGenerator {

  private static double ACURRACY = 0.0000001;

  protected final int numOfElements;
  protected final double alpha;
  protected final Random rnd; // uniform random generator

  public ZipfGenerator(int numOfElements, double alpha) {
    this.numOfElements = numOfElements;
    this.alpha = alpha;
    this.rnd = new Random(System.nanoTime());
    this.init();
  }

  public ZipfGenerator(int numOfElements, double alpha, long randomSeed) {
    this.numOfElements = numOfElements;
    this.alpha = alpha;
    this.rnd = new Random(randomSeed);
    this.init();
  }

  public int getNumOfElements() {
    return this.numOfElements;
  }

  public double getAlpha() {
    return this.alpha;
  }

  /**
   * generate an integer based on zipf distribution return value is in [1,
   * numOfElements]
   */
  public int sample() {
    if (Math.abs(this.alpha - 0.0) < ACURRACY) {
      return this.rnd.nextInt(numOfElements) + 1;
    } else {
      return this.random();
    }
  }

  protected abstract void init();

  protected abstract int random();

  /**
   * return the probability of the given number (rank)
   */
  public abstract double getProbability(int num);

}