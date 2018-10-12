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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.concurrent.ExecutorService;

import Waterloo.MultiSocket.exception.ConnectionCloseException;
import Waterloo.MultiSocket.exception.MultiSocketValidException;

import rpc.execption.MethodNotRegisteredException;
import rpc.execption.NoClientStubException;
import specrpc.client.SpecRpcServerStubObject;
import specrpc.client.api.SpecRpcClient;
import specrpc.client.api.SpecRpcClientStub;
import specrpc.common.Status.SpeculationStatus;
import specrpc.common.api.SpecRpcFacade;
import specrpc.exception.SpeculationFailException;
import specrpc.server.SpecRpcServerStub;
import specrpc.server.api.SpecRpcRollback;

public class SpecRpcFacadeObject implements SpecRpcFacade {
  private final SpecRpcServerStub serverStub;
  private final Status status;
  private final ExecutorService threadPool;
  private boolean isActualReturnMethodCalled;
  // RPC rollback support
  private SpecRpcRollback rollbackObj;
  private boolean isRollbackRegistered = false;
  private boolean isRollbackExecuted = true;

  public SpecRpcFacadeObject(SpecRpcServerStub clientStub, Status status, ExecutorService threadPool) {
    this.serverStub = clientStub;
    this.status = status;
    this.threadPool = threadPool;
    this.isActualReturnMethodCalled = false;
  }

  public synchronized ExecutorService getThreadPool() {
    return this.threadPool;
  }

  // Returns current RPC's status
  public synchronized SpeculationStatus getCurrentRpcStatus() {
    return this.status.getCurrentCallbackStatus();
  }

  public synchronized SpeculationStatus getCallerStatus() {
    return this.status.getCallerStatus();
  }

  public synchronized SpeculationStatus getCalleeStatus() {
    return this.status.getCalleeStatus();
  }

  public synchronized void setCallerStatus(SpeculationStatus callerStatus) {
    this.status.setCallerStatus(callerStatus);
    if (this.status.getCurrentCallbackStatus() != SpeculationStatus.SPECULATIVE) {
      // Wakes up specBlock()
      notifyAll();
      // Notifies CallbackClientStub
      this.serverStub.callbackStatusChanged(this.status.getCurrentCallbackStatus());
    }
  }

  public synchronized void setCalleeStatus(SpeculationStatus calleeStatus) {
    this.status.setCalleeStatus(calleeStatus);
    if (this.status.getCurrentCallbackStatus() != SpeculationStatus.SPECULATIVE) {
      // Wakes up specBlock()
      notifyAll();
      // Notifies CallbackClientStub
      this.serverStub.callbackStatusChanged(this.status.getCurrentCallbackStatus());
    }
  }

  // Blocks to prevent from actual output until the speculation of previous call
  // is finished. If the speculation of previous call failed, throw
  // SpecFailException. Otherwise, just stop blocking.
  @Override
  public synchronized void specBlock() throws SpeculationFailException, InterruptedException {
    // Blocks until callerStatus changed
    while (this.status.getCurrentCallbackStatus() == SpeculationStatus.SPECULATIVE) {
      wait();
    }
    if (this.status.getCurrentCallbackStatus() == SpeculationStatus.FAIL) {
      throw new SpeculationFailException();
    }
  }

  public synchronized boolean isActualReturnMethodCalled() {
    return this.isActualReturnMethodCalled;
  }

  public synchronized void sendReturnToClient(Object rpcReturnValue) throws NoClientStubException,
      SpeculationFailException, InterruptedException, IOException, MultiSocketValidException, ConnectionCloseException {
    if (this.serverStub == null) {
      throw new NoClientStubException();
    }
    this.isActualReturnMethodCalled = true;
    try {
      this.serverStub.sendNonSpecReturn(rpcReturnValue);
    } catch (IOException | MultiSocketValidException | ConnectionCloseException e) {
      // ClientStub.sendNonSpecReturn may firstly speculatively returns in chain
      // pattern. When this spec return happens in a spec callback (iterative
      // pattern), the actual callback may finish early and close the connection. The
      // speculative callback will get I/O exception here (in specRPCFacade).
      // Adding specBlock() here will make sure that I/O exception only happens in
      // SUCCEED status.
      // TODO: Avoids potential deadlock. Such a solution can not solve the problem of
      // that when actual I/O exception happens in spec callback and there will be no
      // SUCCEED change anymore. In this case, it will block here. Deadlock may
      // happen.
      this.specBlock();
      throw e;
    }
  }

  @Override
  public synchronized void throwNonSpecExceptionToClient(String message) throws NoClientStubException,
      SpeculationFailException, InterruptedException, IOException, MultiSocketValidException, ConnectionCloseException {
    if (this.serverStub == null) {
      throw new NoClientStubException();
    }
    this.isActualReturnMethodCalled = true;
    try {
      this.serverStub.throwNonSpecException(message);
    } catch (IOException | MultiSocketValidException | ConnectionCloseException e) {
      this.specBlock();
      throw e;
    }
  }

  @Override
  public synchronized void specReturn(Object rpcReturnValue) throws NoClientStubException,
      SpeculationFailException, InterruptedException, IOException, MultiSocketValidException, ConnectionCloseException {
    if (this.serverStub == null) {
      throw new NoClientStubException();
    }
    try {
      this.serverStub.sendSpecReturn(rpcReturnValue);
    } catch (IOException | MultiSocketValidException | ConnectionCloseException e) {
      this.specBlock();
      throw e;
    }
  }

  // Returns one ServerStub for the specific method signature
  @Override
  public SpecRpcClientStub bind(String serverIdentity, RpcSignature methodSignature)
      throws MethodNotRegisteredException, FileNotFoundException, IOException, SpeculationFailException {

    // Checks if the speculation of the previous call is failed.
    // If yes, we do not need to continue invoking RPC
    if (this.status.getCurrentCallbackStatus() == SpeculationStatus.FAIL) {
      throw new SpeculationFailException();
    }

    // Client.lookup() will cause deadlock as it is a synchronized method.
    Location serverLocation = SpecRpcClient.lookup(serverIdentity, methodSignature);

    return new SpecRpcServerStubObject(methodSignature, serverLocation, this.threadPool, this.status, this.serverStub);
  }

  // Each RPC or Callback object should just register one rollback
  public synchronized void registerRollback(SpecRpcRollback rollbackObj) {
    this.rollbackObj = rollbackObj;
    this.isRollbackRegistered = true;
    this.isRollbackExecuted = false;
  }

  public synchronized boolean isRollbackRegistered() {
    return this.isRollbackRegistered;
  }

  public synchronized void executeRollback() {
    if (this.isRollbackExecuted) {
      return;
    }
    
    if (this.rollbackObj != null) {
      this.rollbackObj.rollback();
    }
    
    this.isRollbackExecuted = true;
  }
}