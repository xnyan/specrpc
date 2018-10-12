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

package specrpc.common.api;

import java.io.FileNotFoundException;
import java.io.IOException;

import Waterloo.MultiSocket.exception.ConnectionCloseException;
import Waterloo.MultiSocket.exception.MultiSocketValidException;

import rpc.execption.MethodNotRegisteredException;
import rpc.execption.NoClientStubException;
import specrpc.client.api.SpecRpcClientStub;
import specrpc.common.RpcSignature;
import specrpc.common.Status.SpeculationStatus;
import specrpc.exception.SpeculationFailException;
import specrpc.server.api.SpecRpcRollback;

/*
 * This interface is used in two kinds:
 * (1) Callback: the specBlock() should block on both callerStatus and calleeStatus
 * (2) Normal Procedure in RPC method: the specBlock() only blocks
 * on the callerStatus, where calleeStatus is SUCCEED. 
 */
public interface SpecRpcFacade {

  // Returns the actual result of RPC to client
  // public abstract void sendReturnToClient(Object rpcReturnValue) throws
  // NoClientStubException, SpecFailException, InterruptedException, IOException,
  // MultiSocketValidException, ConnectionCloseException;

  // Returns the speculative result of RPC to client
  public abstract void specReturn(Object rpcReturnValue) throws NoClientStubException,
      SpeculationFailException, InterruptedException, IOException, MultiSocketValidException, ConnectionCloseException;

  // Returns exception of RPC to client
  public abstract void throwNonSpecExceptionToClient(String message) throws NoClientStubException,
      SpeculationFailException, InterruptedException, IOException, MultiSocketValidException, ConnectionCloseException;

  // Gets the ServerStub for invoking RPCs, which is used in chain and iterative
  // patterns.
  public abstract SpecRpcClientStub bind(String serverIdentity, RpcSignature methodSignature)
      throws MethodNotRegisteredException, FileNotFoundException, IOException, SpeculationFailException;

  // Blocks until current RPC speculation status becomes successful.
  // If the speculation fails, throw a SpecFailException.
  public abstract void specBlock() throws SpeculationFailException, InterruptedException;

  // Returns the speculation status of the current RPC instance
  public abstract SpeculationStatus getCurrentRpcStatus();

  // Returns the Caller's speculation status
  public abstract SpeculationStatus getCallerStatus();

  // Returns the Callee's speculation status
  public abstract SpeculationStatus getCalleeStatus();

  /**
   * Registers a rollback function for the current running RPC instance.
   * 
   * Applications should register its specific rollback function in the
   * implementation of an RPC (on server side).
   * 
   * If the application chooses to use this feature, SpecRPC assumes that the RPC
   * instance finishes its execution without using specBlock to hold speculative
   * execution. This means that the rollback funciton executes after the RPC
   * instance completes.
   */
  public abstract void registerRollback(SpecRpcRollback rollbackObj);

  /**
   * Returns true if there is a rollback function registered
   */
  public abstract boolean isRollbackRegistered();

  /**
   * Executes the rollback function. This will be automatically triggered by
   * SpecRPC when there is a mis-speculation.
   * 
   * TODO: Makes this method not be visible to applications.
   */
  public abstract void executeRollback();

}