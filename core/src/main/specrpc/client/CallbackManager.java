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

package specrpc.client;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import specrpc.client.api.SpecRpcCallback;
import specrpc.client.api.SpecRpcCallbackFactory;
import specrpc.common.CallbackClientStub;
import specrpc.common.SpecRpcFacadeObject;
import specrpc.common.Status;
import specrpc.common.Status.SpeculationStatus;
import specrpc.server.SpecRpcServerStub;

/*
 * CallbacksManager controls callback objects.
 * All the callback objects under a CallbacksManager share the same SpecRPCFacade with the same status. 
 * This status contains callerStatus, calleeStatus, and currentRpcStatus. 
 * The last one is also used to describe is the status of a Callback instance.
 */
public class CallbackManager {

  private SpecRpcCallbackFactory callbackFactory; // factory for creating callback
  private SpeculationStatus callerStatus;
  private final SpecRpcServerStub clientStub;
  private final ExecutorService threadPool;
  private Map<Object, CallbackRunner> callbackRunnerMap; // mapping rpcReturn value to callback thread
  private CallbackRunner correctCallbackRunner; // callback running with the actual rpc return value

  public CallbackManager(SpecRpcCallbackFactory callbackFactory, SpeculationStatus callerStatus,
      SpecRpcServerStub clientStub, ExecutorService threadPool) {
    this.callbackFactory = callbackFactory;
    this.callerStatus = callerStatus;
    this.clientStub = clientStub;
    this.threadPool = threadPool;

    this.callbackRunnerMap = new HashMap<Object, CallbackRunner>();
    this.correctCallbackRunner = null;
  }

  // When the actual result of the RPC returns back, returns true if there is one
  // callback runs with a correct prediction, which is the correctCallbackRunner.
  // Otherwise, returns false.
  public synchronized boolean isCalleeSpecSolved() {
    return (this.correctCallbackRunner != null);
  }

  // Starts one callback instance with a predicted or actual RPC return result.
  // The CalleeStatus determines whether this CallbackRunner is speculative or
  // not.
  public synchronized CallbackRunner runCallback(Object rpcReturnValue, SpeculationStatus calleeStatus) {
    // Create a callback instance
    SpecRpcCallback callback = callbackFactory.createCallback();

    Status callbackStatus = new Status(this.callerStatus, calleeStatus);
    // Changes the specRpcFacade's clientStub to CallbackClientStub for chain RPC
    // pattern. Callbacks use the clientStub. This is designed for chain RPC
    // pattern, where ActualReturnValue may be returned as SpecReturnValue.
    CallbackClientStub callbackClientStub = new CallbackClientStub(this.clientStub,
        callbackStatus.getCurrentCallbackStatus());
    SpecRpcFacadeObject specRpcFacade = new SpecRpcFacadeObject(callbackClientStub, callbackStatus, this.threadPool);
    callbackClientStub.setSpecRPCFacade(specRpcFacade);

    // Runs the callback instance
    CallbackRunner callbackRunner = new CallbackRunner(callback, callbackStatus, specRpcFacade, rpcReturnValue);
    this.threadPool.execute(callbackRunner);

    // Maps the RPC result and the callback instance
    callbackRunnerMap.put(rpcReturnValue, callbackRunner);
    return callbackRunner;
  }

  // Returns the callback instance running with the specified value
  // Returns null if there is no such a callback instance
  public synchronized CallbackRunner getCallbackRunner(Object rpcReturnValue) {
    return callbackRunnerMap.containsKey(rpcReturnValue) ? callbackRunnerMap.get(rpcReturnValue) : null;
  }

  // Returns the Callback instance running with the actual RPC result or correct
  // prediction.
  public synchronized CallbackRunner getCorrectCallbackRunner() {
    return this.correctCallbackRunner;
  }

  // Determines the Callback instance running with correct speculation.
  // If there is no such a Callback instance, creates a Callback instance with the
  // actual RPC return result and returns this callback instance.
  public synchronized CallbackRunner determineCorrectCallbackRunner(Object actualRpcReturnValue) {
    CallbackRunner correctRunner = null;
    // Finds the Callback instance running with correct speculation, and invalidates others.
    for (Map.Entry<Object, CallbackRunner> entry : callbackRunnerMap.entrySet()) {
      CallbackRunner runner = entry.getValue();
      // The actual RPC return value may be null.
      if ((actualRpcReturnValue == null && entry.getKey() == null)
          || (actualRpcReturnValue != null && actualRpcReturnValue.equals(entry.getKey()))) {
        correctRunner = runner;
        // Correct Speculation
        correctRunner.setCalleeStatus(SpeculationStatus.SUCCEED);
      } else {
        // Invalidates Callback instances that have incorrect speculations.
        runner.setCalleeStatus(SpeculationStatus.FAIL);
      }
    }

    this.callbackRunnerMap.clear();

    if (correctRunner != null) {
      this.correctCallbackRunner = correctRunner;
      this.callbackRunnerMap.put(actualRpcReturnValue, this.correctCallbackRunner);
    } else {
      this.correctCallbackRunner = this.runCallback(actualRpcReturnValue, SpeculationStatus.SUCCEED);
    }

    return this.correctCallbackRunner;
  }

  // When RPC returns an exception, all callback instances should be invalidated
  public synchronized void invalidateAllCallbackRunners() {
    for (CallbackRunner callbackRunner : this.callbackRunnerMap.values()) {
      callbackRunner.setCalleeStatus(SpeculationStatus.FAIL);
    }
    this.callbackRunnerMap.clear();
  }

  // The Control Thread is responsible for notifying CallbacksManager
  public synchronized void callerStatusChanged(SpeculationStatus callerStatus) {
    if (this.callerStatus != callerStatus) {
      this.callerStatus = callerStatus;
      for (CallbackRunner callbackRunner : this.callbackRunnerMap.values()) {
        // Notifies each callback instance that callerStatus changed
        callbackRunner.setCallerStatus(callerStatus);
      }
    }
  }
}
