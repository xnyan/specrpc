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

package micro.server.specrpc;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import Waterloo.MultiSocket.exception.ConnectionCloseException;
import Waterloo.MultiSocket.exception.MultiSocketValidException;
import micro.common.MicroConstants;
import micro.server.MicroServer;
import micro.server.MicroService;
import rpc.execption.MethodNotRegisteredException;
import rpc.execption.NoClientStubException;
import rpc.execption.UserException;
import specrpc.client.api.SpecRpcClient;
import specrpc.client.api.SpecRpcClientStub;
import specrpc.common.api.SpecRpcFacade;
import specrpc.exception.SpeculationFailException;
import specrpc.server.api.SpecRpcHost;

public class MicroServiceSpecRpc extends MicroService implements SpecRpcHost {

  private static final Logger logger = LoggerFactory.getLogger(MicroConstants.LOGGER_TYPE);

  private static final MicroServerCompCallbackFactory compCBFactory = new MicroServerCompCallbackFactory();

  private SpecRpcFacade specRpcFacade;

  public MicroServiceSpecRpc() {

  }

  @Override
  public void bind(SpecRpcFacade specRPCFacade) {
    this.specRpcFacade = specRPCFacade;
  }

  @Override
  public String multiHop(String data, Integer hopNum) {

    if (MicroServer.isPredict() && MicroServer.getPredictPoint() == MicroConstants.PREDICT_POINT.BEFORE_ANY) {
      this.doPrediction(data);
    }

    this.doComputation(MicroServer.getComputationTimeBeforeRPC());

    if (MicroServer.isPredict() && MicroServer.getPredictPoint() == MicroConstants.PREDICT_POINT.BEFORE_RPC) {
      this.doPrediction(data);
    }
    
    if (hopNum > 0) {
      // More RPCs to call
      try {

        SpecRpcClientStub rpcClientStub = SpecRpcClient.bind(MicroServer.getNextHopServerId(),
            MicroService.RPC_MULTI_HOP);
        // Computation after RPC is done in callback
        rpcClientStub.call(null, MicroServiceSpecRpc.compCBFactory, data, hopNum - 1).getResult(); // blocking call

      } catch (MethodNotRegisteredException | IOException | InterruptedException e) {
        e.printStackTrace();
        logger.error(e.getMessage());
        System.exit(MicroConstants.RUNTIME_FATAL_ERROR_CODE);
      } catch (SpeculationFailException e1) {
        ; // does nothing for this
      } catch (UserException e) {
        try {
          this.specRpcFacade.throwNonSpecExceptionToClient(e.getMessage());
        } catch (NoClientStubException | InterruptedException | IOException | MultiSocketValidException
            | ConnectionCloseException ee1) {
          ee1.printStackTrace();
          logger.error(ee1.getMessage());
          System.exit(MicroConstants.RUNTIME_FATAL_ERROR_CODE);
        } catch (SpeculationFailException ee2) {
          ;
        }
      }
    } else {
      // The last RPC
      if (MicroServer.isPredict() && MicroServer.getPredictPoint() == MicroConstants.PREDICT_POINT.AFTER_RPC) {
        this.doPrediction(data);
      }
      this.doComputation(MicroServer.getComputationTimeAfterRPC());
    }

    return data;
  }

  protected void doPrediction(String data) {
    try {
      int correctP = MicroServer.getRandomVar().nextInt(MicroConstants.PERCENTAGE); // [0,99]
      if (correctP < MicroServer.getCorrectPredictRate()) {
        // Correct prediction
        this.specRpcFacade.specReturn(data);
      } else {
        // Incorrect prediction
        this.specRpcFacade.specReturn(MicroConstants.INCORRECT_PREDICTION_DATA);
      }
    } catch (NoClientStubException | InterruptedException | IOException | MultiSocketValidException
        | ConnectionCloseException e) {
      e.printStackTrace();
      logger.error(e.getMessage());
      System.exit(MicroConstants.RUNTIME_FATAL_ERROR_CODE);
    } catch (SpeculationFailException e1) {
      ;// does nothing
    }
  }
}
