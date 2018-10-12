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

package rc.server.db.lock;

import java.util.HashSet;

/**
 * Thread-safe HashSet
 */
public class ConcurrentHashSet<E> {

  private HashSet<E> hashSet;
  
  public ConcurrentHashSet() {
    this.hashSet = new HashSet<E>();
  }
  
  public synchronized void add(E element) {
    this.hashSet.add(element);
  }
  
  /**
   * Removes the given element.
   * 
   * @param element
   * @return True if the set contains the element. Otherwise, false.
   */
  public synchronized Boolean remove(E element) {
    return this.hashSet.remove(element);
  }
  
  public synchronized Boolean contains(E element) {
    return this.hashSet.contains(element);
  }
  
  public synchronized void clear() {
    this.hashSet.clear();
  }
}
