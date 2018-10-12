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

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;

/*
 * FixedSizeHashQueue provides a limited-size data structure behaves: 
 * Element is added at the end of queue;
 * Testing if an element exists needs O(1) time;
 * When it is full, adding new element will pop out the oldest one; 
 * 
 * This structure is not synchronized.
 */
public class FixedSizeHashQueue {
  private HashSet<String> dataSet;
  private Queue<String> queue;
  private final int size;

  public FixedSizeHashQueue(int size) {
    this.size = size;
    this.dataSet = new HashSet<String>();
    this.queue = new LinkedList<String>();
  }

  public void add(String element) {
    if (this.dataSet.contains(element))
      return;
    this.dataSet.add(element);
    this.queue.add(element);
    while (this.queue.size() > this.size) {
      this.dataSet.remove(this.queue.poll());
    }
  }

  public boolean contains(String element) {
    return this.dataSet.contains(element);
  }

  public void clear() {
    this.dataSet.clear();
    this.queue.clear();
  }

  public String toString() {
    String re = "Max Size = " + this.size + ", DataSet Current Size = " + this.dataSet.size()
        + ", Queue Current Size = " + this.queue.size();
    re += "\nDataSet Elements: ";
    Iterator<String> itr = this.dataSet.iterator();
    while (itr.hasNext()) {
      re += itr.next() + ",";
    }

    re += "\nQueue Elements: ";
    itr = this.queue.iterator();
    while (itr.hasNext()) {
      re += itr.next() + ",";
    }
    return re;
  }
}
