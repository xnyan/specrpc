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

public class Units {

  public enum DATA_SIZE {
    bit(1),
    Byte(8 * bit.size), 
    Kb(1000 * Byte.size), 
    Mb(1000 * Kb.size), 
    Gb(1000 * Mb.size), 
    KB(1024 * Byte.size), 
    MB(1024 * Byte.size), 
    GB(1024 * Byte.size);

    public final long size;

    private DATA_SIZE(long size) {
      this.size = size;
    }
  };

  public enum LENGTH {
    METER(1), KM(1000 * METER.length);

    public final int length;

    private LENGTH(int length) {
      this.length = length;
    }
  }

  public enum TIME {
    MILLI_SECOND(1), SECOND(1000 * MILLI_SECOND.period);

    public final long period;

    private TIME(long period) {
      this.period = period;
    }
  }
}
