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

package specrpc.server;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import Waterloo.MultiSocket.exception.ConnectionCloseException;
import Waterloo.MultiSocket.exception.MultiSocketValidException;

import rpc.execption.MethodNotRegisteredException;
import rpc.execption.NoClientStubException;
import specrpc.common.RpcSignature;
import specrpc.common.SpecRpcFacadeObject;
import specrpc.exception.SpeculationFailException;
import specrpc.server.api.SpecRpcHost;

public class SpecRpcExecutor implements Runnable {

  private final RpcSignature signature;
  private final Object[] args;
  private final SpecRpcHost hostObject;
  private final SpecRpcFacadeObject specRpcFacade;
  private boolean finished;

  public SpecRpcExecutor(SpecRpcFacadeObject specRpcFacade, SpecRpcHost hostObject, RpcSignature signature,
      Object[] args) {
    this.hostObject = hostObject;
    this.signature = signature;
    this.args = args;
    this.specRpcFacade = specRpcFacade;
    this.finished = false;
  }

  @Override
  public synchronized void run() {
    try {
      // Executes the RPC method
      callMethod();
    } catch (MethodNotRegisteredException | IllegalAccessException | InvocationTargetException e) {
      // TODO: notifies the client that an exception happened
      e.printStackTrace();
    } finally {
      finished = true;
      notifyAll();
    }
  }

  public synchronized void join() throws InterruptedException {
    while (finished == false) {
      wait();
    }
  }

  private void callMethod() throws MethodNotRegisteredException, IllegalAccessException, InvocationTargetException {
    Method method = null;
    try {
      Class<? extends SpecRpcHost> hostObjClass = hostObject.getClass();
      method = hostObjClass.getMethod(signature.methodName, signature.argTypes);
      Object result = method.invoke(hostObject, args);
      if (!this.specRpcFacade.isActualReturnMethodCalled()) {
        // Returns final RPC response
        this.specRpcFacade.sendReturnToClient(result);
      }
    } catch (NoSuchMethodException e) {
      throw new MethodNotRegisteredException(signature);
    } catch (InvocationTargetException e) {
      checkForFailSpecException(e);
    } catch (SpeculationFailException e) {
      // Incorrect speculation (i.e., RPC caller's speculation status becomes FAIL)
      ;
    } catch (NoClientStubException | InterruptedException | IOException | MultiSocketValidException
        | ConnectionCloseException e) {
      e.printStackTrace();
    }
  }

  private void checkForFailSpecException(InvocationTargetException e) throws InvocationTargetException {
    if (!(e.getTargetException() instanceof SpeculationFailException)) {
      throw e;
    }
  }
}
