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

package Waterloo.MultiSocket;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class BasicFuture<T> implements Future<T> {
  private T val = null;
  private boolean done = false;

  public boolean cancel(boolean mayInterruptIfRunning) {
    return false;
  }

  public synchronized T get() throws InterruptedException, ExecutionException {
    while (!done) {
      wait();
    }
    return val;
  }

  public synchronized T get(long timeout, TimeUnit unit)
      throws InterruptedException, ExecutionException, TimeoutException {
    assert (false); // Not currently supported
    return null;
  }

  public synchronized void put(T val) {
    this.val = val;
    done = true;
    notifyAll();
  }

  public boolean isCancelled() {
    return false;
  }

  public synchronized boolean isDone() {
    return done;
  }
}
