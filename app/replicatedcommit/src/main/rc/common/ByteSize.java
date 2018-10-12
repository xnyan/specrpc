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

package rc.common;

public class ByteSize {

  public enum Unit {
    B, K, M, G, T
  };

  public static final long carry = 1024;

  public long size;
  public Unit unit;

  public ByteSize(long size, Unit unit) {
    this.size = size;
    this.unit = unit;
  }

  // TODO detects long number overflow
  public long getByteSize() {
    long result = size;
    switch (this.unit) {
    case T:
      result *= carry;
    case G:
      result *= carry;
    case M:
      result *= carry;
    case K:
      result *= carry;
    case B:
      break;
    }
    return result;
  }

  public static ByteSize parseByteSize(String str) throws FormatException {
    if (str != null && str.length() != 0) {
      String text = str.trim();
      int numIndex = 0;
      for (numIndex = 0; numIndex < text.length(); numIndex++) {
        if (text.charAt(numIndex) < '0' || text.charAt(numIndex) > '9') {
          break;
        }
      }

      if (numIndex > 0 && numIndex < text.length()) {
        String unitStr = text.charAt(numIndex) + "";
        String numStr = text.substring(0, numIndex);
        Unit unit = null;
        long num = 0;
        try {
          unit = Unit.valueOf(unitStr.toUpperCase());
          num = Long.parseLong(numStr.toString());
        } catch (IllegalArgumentException e) {
          throw new FormatException("Can not parse " + text + " into Byte size.\n" + e.getMessage());
        }

        ByteSize byteSize = new ByteSize(num, unit);
        return byteSize;
      }
    }

    throw new FormatException("Can not parse " + str + " into Byte size.");
  }
}
