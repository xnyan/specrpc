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

import java.io.IOException;

import Waterloo.MultiSocket.exception.ConnectionCloseException;
import Waterloo.MultiSocket.exception.MultiSocketValidException;
import rpc.execption.NoClientStubException;
import specrpc.common.Status.SpeculationStatus;
import specrpc.exception.SpeculationFailException;
import specrpc.server.SpecRpcServerStub;

/*
 * CallbackClientStub is used in the SpecRPCFacade in callback.
 * 
 * This one is used in Chain RPC Pattern, especially where we
 * need to send back to the caller the result of RPC in current
 * callback. 
 */
public class CallbackClientStub implements SpecRpcServerStub {

  private final SpecRpcServerStub clientStub;
  private SpeculationStatus callbackStatus;

  // Uses specBlock() when necessary to release the lock on specRPCFacade in
  // order to avoid deadlock between status change of SpecRPCFacade and any
  // wait() in this CallbackClientStub
  public SpecRpcFacadeObject specRpcFacade;
  private boolean isSpecRpcFacadeSet;

  public CallbackClientStub(SpecRpcServerStub clientStub, SpeculationStatus status) {
    this.clientStub = clientStub;
    this.callbackStatus = status;
    this.isSpecRpcFacadeSet = false;
  }

  // This method is only called once and must be called after initialization
  public synchronized void setSpecRPCFacade(SpecRpcFacadeObject specRPCFacade) {
    if (this.isSpecRpcFacadeSet) {
      return;
    }

    this.specRpcFacade = specRPCFacade;
    this.isSpecRpcFacadeSet = true;
  }

  private Object nonSpecReturnSentAsSpec;
  private boolean aNonSpecReturnWasSentAsSpec = false;

  @Override
  public synchronized void sendNonSpecReturn(Object value) throws NoClientStubException, SpeculationFailException,
      InterruptedException, IOException, MultiSocketValidException, ConnectionCloseException {
    if (this.clientStub == null) {
      throw new NoClientStubException();
    }

    switch (this.callbackStatus) {
    case SPECULATIVE:
      this.nonSpecReturnSentAsSpec = value;
      this.aNonSpecReturnWasSentAsSpec = true;
      this.clientStub.sendSpecReturn(value);
      break;
    case SUCCEED:
      this.clientStub.sendNonSpecReturn(value);
      break;
    case FAIL:
      throw new SpeculationFailException();
    }
  }

  @Override
  public synchronized void sendSpecReturn(Object value) throws NoClientStubException, SpeculationFailException,
      InterruptedException, IOException, MultiSocketValidException, ConnectionCloseException {

    if (this.clientStub == null) {
      throw new NoClientStubException();
    }

    if (this.callbackStatus == SpeculationStatus.FAIL) {
      throw new SpeculationFailException();
    }
    try {
      this.clientStub.sendSpecReturn(value);
    } catch (IOException e) {
      throw e;
    } catch (MultiSocketValidException e) {
      throw e;
    } catch (ConnectionCloseException e) {
      throw e;
    }
  }

  @Override
  // NOTE: do not use synchronized here to avoid deadlock between this and
  // statusChanged
  public void throwNonSpecException(String message) throws NoClientStubException, SpeculationFailException,
      InterruptedException, IOException, MultiSocketValidException, ConnectionCloseException {
    if (this.clientStub == null) {
      throw new NoClientStubException();
    }

    // specBlock here because speculative callback may call this method
    this.specRpcFacade.specBlock();// notified by the status change of specRPCFacade but not by this object itself

    /*
     * Can not wait on CallbackClientStub's monitor because deadlock happens on
     * SpecRPCFacade's monitor
     * 
     * while (this.callbackStatus == SpeculationStatus.SPECULATIVE) { wait(); }
     * 
     * if (this.callbackStatus == SpeculationStatus.FAIL) { throw new
     * SpecFailException(); }
     */

    // This will return a non spec exception message to Client side
    this.clientStub.throwNonSpecException(message);
  }

  // When callback's status change, this method is callbed by SpecRPCFacade
  public synchronized void callbackStatusChanged(SpeculationStatus callbackStatus) {
    if (this.callbackStatus != callbackStatus) {
      this.callbackStatus = callbackStatus;
      if (aNonSpecReturnWasSentAsSpec && this.callbackStatus == SpeculationStatus.SUCCEED) {
        try {
          // Debug Info
          // System.out.println("Debug Info : " + nonSpecReturnSentAsSpec.toString() + "@
          // callbackStatusChanged() in CallbackClientStub.java");
          this.sendNonSpecReturn(nonSpecReturnSentAsSpec);
        } catch (NoClientStubException | SpeculationFailException | InterruptedException | IOException
            | MultiSocketValidException | ConnectionCloseException e) {
          // TODO: Use a better way to handle exceptions
          e.printStackTrace();
        }
      }
      // TODO: Removes this since there should be nothing waiting on this object
      notifyAll();
    }
  }

}
