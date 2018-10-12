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

public class TxnReadResult {

  public final String val;
  public final Long version;
  public final boolean isSharedLockAcquired;
  
  public TxnReadResult(String val, Long version, boolean isSharedLockAcquired) {
    this.val = val;
    this.version = version;
    this.isSharedLockAcquired = isSharedLockAcquired;
  }
  
  public String toString() {
    return "(value = " + this.val + 
        ", version = " + this.version + 
        ", isSharedLockAcquired = " + this.isSharedLockAcquired + 
        ")";
  }
  
  public boolean equals(Object obj) {
    
    if (obj == null) {
      // This must not be null, otherwise, equals() can not execute.
      return false;
    }
    
    TxnReadResult res = (TxnReadResult) obj;
    
    if (this.isSharedLockAcquired != res.isSharedLockAcquired) {
      return false;
    }
    
    if ((this.version == null && res.version != null)) {
      return false;
    }
    
    if (this.version == null && res.version == null) {
      return true;
    }

    if (! this.version.equals(res.version)) {
      return false;
    }
    
    // We may do not need to check values, since the txn system must guarantee their
    // equivalence based on versions.
    if ((this.val == null && res.val != null)) {
      return false;
    }
    
    if (this.val == null && res.val == null) {
      return true;
    }
    
    if (! this.val.equals(res.val)) {
      return false;
    }
    
    return true;
  }
}
