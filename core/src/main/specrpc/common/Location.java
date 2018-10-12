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

package specrpc.common;

public class Location {

  public static final String SEPERATOR = ":";

  final public String hostname;
  final public int port;

  public Location(String hostname, int port) {
    this.hostname = hostname;
    this.port = port;
  }

  public Location(String str) {
    String[] parts = str.split(SEPERATOR);
    this.hostname = parts[0];
    this.port = Integer.parseInt(parts[1]);
  }

  public String toString() {
    return this.hostname + SEPERATOR + this.port;
  }
}
