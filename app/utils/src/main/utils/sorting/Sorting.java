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

package utils.sorting;

import java.util.ArrayList;

public class Sorting {

  public static ArrayList<String> sort(ArrayList<String> list) {
    ArrayList<String> result = new ArrayList<String>();
    int[] elements = new int[list.size()];
    for (int i = 0; i < list.size(); i++) {
      elements[i] = Integer.parseInt(list.get(i));
    }
    quickSort(elements, 0, elements.length - 1);
    for (int i = 0; i < elements.length; i++) {
      result.add(elements[i] + "");
    }
    return result;
  }

  public static void quickSort(int[] list, int start, int end) {
    if (end <= start)
      return;
    // use the start as axis
    int axis = start;
    for (int i = axis + 1; i <= end; i++) {
      if (list[i] <= list[axis]) {
        swap(list, i, axis);
        swap(list, i, axis + 1);
        axis = axis + 1;
      }
    }
    quickSort(list, axis + 1, end);
    quickSort(list, start, axis - 1);
  }

  public static void swap(int[] list, int first, int second) {
    if (first == second)
      return;
    int temp = list[first];
    list[first] = list[second];
    list[second] = temp;
  }
}
