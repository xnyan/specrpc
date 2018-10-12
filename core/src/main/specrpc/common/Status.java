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

import java.util.ArrayList;
import java.util.List;

import specrpc.client.api.SpecRpcStatistics;

// Each ServerStub has only one CallbacksManager
// Each CallbacksManager has only one Status
// All the Callbacks under the same CallbacksManager share the same Status
// Status contains: callerStatus, currentRPCStatus, calleeStatus
public class Status {

  public enum SpeculationStatus {
    SUCCEED, FAIL, SPECULATIVE
  };

  private SpeculationStatus callerStatus;// caller's status
  private SpeculationStatus currentCallbackStatus;// current callback's status
  private SpeculationStatus calleeStatus;// callee's status

  // Statistics
  private final boolean isRpcResultPredicted;

  // When current RPC status changed, the status's listeners should be notified
  // StatusListeners contains: (1) Server; (2) next level callback
  // Listeners may take this status as callerStatus or currentRPCStatus
  private final List<StatusListener> listeners = new ArrayList<StatusListener>();

  public Status(SpeculationStatus callerStatus, SpeculationStatus calleeStatus) {
    this.callerStatus = callerStatus;
    this.calleeStatus = calleeStatus;
    this.isRpcResultPredicted = this.calleeStatus == SpeculationStatus.SPECULATIVE ? true : false;
    this.updateCurrentSPCStatus(); // Sets currentRPCStatus
  }

  public synchronized SpeculationStatus getCallerStatus() {
    return this.callerStatus;
  }

  public synchronized void setCallerStatus(SpeculationStatus callerStatus) {
    if (SpecRpcStatistics.isEnabled && this.isRpcResultPredicted) {
      if (this.callerStatus == SpeculationStatus.SPECULATIVE && callerStatus == SpeculationStatus.SUCCEED
          && this.calleeStatus == SpeculationStatus.SUCCEED) {
        SpecRpcStatistics.increaseCorrectPredictionNumber();
      } else if (SpecRpcStatistics.isCountingIncorrectPrediction && this.currentCallbackStatus != SpeculationStatus.FAIL
          && callerStatus == SpeculationStatus.FAIL) {
        SpecRpcStatistics.increaseIncorrectPredictionNumber();
      }
    }

    // If callerStatus changed, updates current RPC Status.
    if (this.callerStatus != callerStatus) {
      this.callerStatus = callerStatus;
      SpeculationStatus oldStatus = this.currentCallbackStatus;
      updateCurrentSPCStatus();

      // If current RPC Status changed, notifies listeners.
      if (this.currentCallbackStatus != oldStatus) {
        notifyListeners();
      }
    }
  }

  public synchronized SpeculationStatus getCalleeStatus() {
    return this.calleeStatus;
  }

  public synchronized void setCalleeStatus(SpeculationStatus calleeStatus) {
    if (SpecRpcStatistics.isEnabled && this.isRpcResultPredicted) {
      if (this.calleeStatus == SpeculationStatus.SPECULATIVE && calleeStatus == SpeculationStatus.SUCCEED
          && this.callerStatus == SpeculationStatus.SUCCEED) {
        SpecRpcStatistics.increaseCorrectPredictionNumber();
      } else if (SpecRpcStatistics.isCountingIncorrectPrediction && this.currentCallbackStatus != SpeculationStatus.FAIL
          && calleeStatus == SpeculationStatus.FAIL) {
        SpecRpcStatistics.increaseIncorrectPredictionNumber();
      }
    }
    // If calleeStatus changed, updates current RPC Status.
    if (this.calleeStatus != calleeStatus) {
      this.calleeStatus = calleeStatus;
      SpeculationStatus oldStatus = this.currentCallbackStatus;
      updateCurrentSPCStatus();

      // If current RPC Status changed, notifies listeners.
      if (this.currentCallbackStatus != oldStatus) {
        notifyListeners();
      }
    }
  }

  public synchronized SpeculationStatus getCurrentCallbackStatus() {
    return this.currentCallbackStatus;
  }

  // When callerStatus and calleeStatus changes, the currentRPCStatus needs to
  // change. This method is only called by setCallerStatus() and
  // setCalleeStatus().
  private synchronized void updateCurrentSPCStatus() {
    if (callerStatus == SpeculationStatus.FAIL || calleeStatus == SpeculationStatus.FAIL) {
      this.currentCallbackStatus = SpeculationStatus.FAIL;
    } else if (callerStatus == SpeculationStatus.SUCCEED && calleeStatus == SpeculationStatus.SUCCEED) {
      this.currentCallbackStatus = SpeculationStatus.SUCCEED;
    } else {
      this.currentCallbackStatus = SpeculationStatus.SPECULATIVE;
    }
  }

  private synchronized void notifyListeners() {
    for (StatusListener statusListener : this.listeners) {
      statusListener.statusChanged(this.currentCallbackStatus);
    }
  }

  public synchronized void addListener(StatusListener listener) {
    this.listeners.add(listener);
  }
}
