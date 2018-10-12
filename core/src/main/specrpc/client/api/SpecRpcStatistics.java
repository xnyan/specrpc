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

package specrpc.client.api;

import java.util.concurrent.atomic.AtomicLong;

/*
 * Statistics counts SpecRPC run-time statistics, such as prediction correctness
 */
public class SpecRpcStatistics {

  public static boolean isEnabled = false; // Not thread-safe
  public static boolean isCountingIncorrectPrediction = false;

  // Counts prediction correctness
  private static AtomicLong TOTAL_PREDICTION_NUMBER = new AtomicLong(0); // both client/server prediction
  private static AtomicLong CORRECT_PREDICTION_NUMBER = new AtomicLong(0); // both Caller/Callee speculation correct
  private static AtomicLong INCORRECT_PREDICTION_NUMBER = new AtomicLong(0);

  public static void setIsEnabled(boolean enable) {
    isEnabled = enable;
  }

  public static boolean isEnabled() {
    return isEnabled;
  }

  public static void setIsCountingIncorrectPrediction(boolean isCount) {
    isCountingIncorrectPrediction = isCount;
  }

  public static boolean isCountingIncorrectPrediction() {
    return isCountingIncorrectPrediction;
  }

  public static void reset() {
    TOTAL_PREDICTION_NUMBER.set(0);
    CORRECT_PREDICTION_NUMBER.set(0);
    INCORRECT_PREDICTION_NUMBER.set(0);
  }

  public static void increaseTotalPredictionNumber() {
    TOTAL_PREDICTION_NUMBER.incrementAndGet();
  }

  public static void increaseCorrectPredictionNumber() {
    CORRECT_PREDICTION_NUMBER.incrementAndGet();
  }

  public static void increaseIncorrectPredictionNumber() {
    INCORRECT_PREDICTION_NUMBER.incrementAndGet();
  }

  public static synchronized long getTotalPredictionNumber() {
    return TOTAL_PREDICTION_NUMBER.get();
  }

  public static synchronized long getCorrectPredictionNumber() {
    return CORRECT_PREDICTION_NUMBER.get();
  }

  public static synchronized long getIncorrectPredictionNumber() {
    return INCORRECT_PREDICTION_NUMBER.get();
  }
}
