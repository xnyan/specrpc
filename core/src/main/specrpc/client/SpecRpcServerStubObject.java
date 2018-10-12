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

import java.util.List;
import java.util.concurrent.ExecutorService;

import specrpc.client.api.SpecRpcCallbackFactory;
import specrpc.client.api.SpecRpcFuture;
import specrpc.client.api.SpecRpcClientStub;
import specrpc.client.api.SpecRpcStatistics;
import specrpc.common.Location;
import specrpc.common.RpcSignature;
import specrpc.common.Status;
import specrpc.common.Status.SpeculationStatus;
import specrpc.exception.SpeculationFailException;
import specrpc.server.SpecRpcServerStub;

public class SpecRpcServerStubObject implements SpecRpcClientStub {

  private final RpcSignature signature;
  private final Location serverLocation;
  private final ExecutorService clientThreadPool;

  // CallbacksManager and ControlThread will listen on the callerStatus
  // CallbacksManager is responsible for notifying the callback instances.
  // ControlThread is responsible for notifying the RPC server.
  private final Status callerStatus;

  // ClientStub for returning result to previous RPC call
  // This is combined into SpecRpcFacade and is used only in chain RPC pattern
  private final SpecRpcServerStub clientStub;

  public SpecRpcServerStubObject(RpcSignature signature, Location serverLocation, ExecutorService clientThreadPool,
      Status callerStatus, SpecRpcServerStub clientStub) {
    this.signature = signature;
    this.serverLocation = serverLocation;

    this.clientThreadPool = clientThreadPool;
    this.clientStub = clientStub;

    this.callerStatus = callerStatus;
  }

  @Override
  public SpecRpcFuture call(List<Object> predictedValues, SpecRpcCallbackFactory factory, Object... args)
      throws SpeculationFailException {
    // Future structure contains the result or exception of the correct callback
    SpecRpcFutureObject future = null;
    CallbackManager callbacksManager = null;

    // Before ControlThread listens on callerStatus, every object depends on
    // callerStatus.
    // TODO: Would it be better to use this initialCallerStatus to get a
    // synchronization?
    SpeculationStatus initialCallerStatus = this.callerStatus.getCurrentCallbackStatus();

    // Checks the status of the caller (i.e., the callback the RPC depends on).
    if (initialCallerStatus == SpeculationStatus.FAIL) {
      throw new SpeculationFailException();
    }

    // Creates one callbacksManager
    // ClientStub is null for the first ServerStub.call()
    callbacksManager = new CallbackManager(factory, initialCallerStatus, this.clientStub, this.clientThreadPool);

    if (predictedValues != null) {
      // Starts running callback(s) with the speculative values
      for (Object value : predictedValues) {
        callbacksManager.runCallback(value, SpeculationStatus.SPECULATIVE);
        if (SpecRpcStatistics.isEnabled) {
          SpecRpcStatistics.increaseTotalPredictionNumber();
        }
      }
    }

    // After getting the result or exception of callback puts the result or
    // exception into Future.
    future = new SpecRpcFutureObject(initialCallerStatus);

    // ControlThread will invoke RPC. ControlThread must consume the
    // initialCallerStatus instead of current callerStatus because some objects
    // under ControlThread only know the initialCallerStatus. If ControlThread
    // consume current callerStatus, these objects will not be notified the change,
    // and then deadlock will happen.
    ControlThread controlThread = new ControlThread(this.signature, args, this.serverLocation, callbacksManager, future,
        initialCallerStatus, this.clientThreadPool);

    // ControlThread listen on the callerStatus
    this.callerStatus.addListener(controlThread);

    // Checks if status changed before controlThread listen on it because
    // CallbacksManager may create callbacks based on old callerStatus, but
    // callerStatus changed before controlThread listen on it.

    controlThread.statusChanged(this.callerStatus.getCurrentCallbackStatus());

    // Execute the controlThread at last because
    // the statusChanged() competes with the run(),
    // which may delay the future return when run()
    // happens before statusChanged() since in run()
    // there is blocking network connection
    // TODO: checks if this invalids the correctness
    this.clientThreadPool.execute(controlThread);

    return future;
  }

}
