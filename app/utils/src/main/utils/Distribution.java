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

public class Distribution {
  private Random rand;

  public Distribution(Random random) {
    rand = random;
  }

  public Distribution(long seed) {
    rand = new Random(seed);
  }

  public double expntl(double mean, double min, double max) {
    double y, time;
    mean -= min;
    do {
      while ((y = rand.nextDouble()) == 0.0)
        ;
      time = (-mean) * Math.log(y);
    } while (min + time > max);
    return (min + time);
  }

  public double poisson(double mean) {
    double n = 0.0;
    // Approximate poisson distribution using normal distribution
    // for large mean values.
    if (mean > 6.0) {
      return (normal(mean, Math.sqrt(mean), 0.0, Double.MAX_VALUE));
    }
    double y = Math.exp(-1 * mean);
    double x = rand.nextDouble();
    while (x >= y) {
      n += 1.0;
      x = x * rand.nextDouble();
    }
    return n;
  }

  public double normal(double mean, double stdev, double min, double max) {
    double q, r, s, x, xx = 0, y, yy = 0;
    do {
      s = 2.0;
      while (s > 1.0) {
        x = rand.nextDouble();
        y = (2.0 * rand.nextDouble()) - 1;
        xx = x * x;
        yy = y * y;
        s = xx + yy;
      }
      // Make sure that the next random number does not equal to 0.0
      while ((x = rand.nextDouble()) == 0.0)
        ;
      r = Math.sqrt((-2.0) * Math.log(x)) / s;
      q = r * stdev * (xx - yy) + mean;
    } while (q < min || q > max);
    return q;
  }

  public static void main(String[] args) {
    Distribution test = new Distribution(1);
    double sum = 0.0;
    int count = 0;
    for (int i = 0; i < 100000; ++i) {
      double tmp = test.expntl(50, 20, 1500);
      sum += tmp;
      count++;
      System.out.println(i + " is " + tmp);
    }
    System.out.println("The average is: " + sum / count);
  }
}
