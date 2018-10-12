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

public class SequentialGenerator extends NumberGenerator {

  private final int start;
  private final int end;
  private int next;

  public SequentialGenerator(int start, int end) {
    this.start = start;
    this.end = end;
    next = start;
  }

  @Override
  public synchronized int sample() {
    int result = next;
    next++;
    if (next > end)
      next = start;
    return result;
  }
}
