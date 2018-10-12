package micro.client.specrpc;

import java.io.IOException;
import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import micro.client.request.MicroRequest;
import micro.common.MicroConstants;
import micro.common.MicroServerIdService;
import micro.common.specrpc.MicroSpecRandom;
import micro.server.MicroService;
import rpc.execption.MethodNotRegisteredException;
import rpc.execption.UserException;
import specrpc.client.api.SpecRpcCallback;
import specrpc.client.api.SpecRpcFuture;
import specrpc.client.api.SpecRpcClientStub;
import specrpc.common.api.SpecRpcFacade;
import specrpc.exception.SpeculationFailException;

public class MicroClientSpecRpcCallback implements SpecRpcCallback {

  private static final Logger logger = LoggerFactory.getLogger(MicroConstants.LOGGER_TYPE);

  private SpecRpcFacade specRpcFacade;
  
  private MicroRequest request;
  private int curRpcIndex; // [2, n]
  
  public MicroClientSpecRpcCallback(MicroRequest req, int curRpcIdx) {
    this.request = req;
    this.curRpcIndex = curRpcIdx;
  }

  @Override
  public void bind(SpecRpcFacade specRPCFacade) {
    this.specRpcFacade = specRPCFacade;
  }

  @Override
  public Object run(Object rpcReturnValue) throws SpeculationFailException, InterruptedException {
    // Local computation
    int prevRpcIndex = this.curRpcIndex - 1;
    this.doLocalComputation(this.request.getLocalCompTime(prevRpcIndex));
    
    // If there are no more RPCs
    if (this.curRpcIndex > request.getRpcNum()) {
      return rpcReturnValue;
    }
    // Next RPC
    String ret = null;
    String serverId = MicroServerIdService.getServerId(this.curRpcIndex);

    SpecRpcClientStub rpcClientStub = null;
    SpecRpcFuture future = null;
    // Callbacks should move to the next RPC
    MicroClientSpecRpcCallbackFactory rpcFactory = new MicroClientSpecRpcCallbackFactory(this.request, this.curRpcIndex + 1);

    // Predictions
    ArrayList<Object> predictionList = null;
    if (MicroClientSpecRpc.isPredict()) {
      // Does prediction if configured
      predictionList = new ArrayList<Object>();
      int correctP = MicroSpecRandom.getPercent();
      if (correctP < MicroClientSpecRpc.getCorrectPredictRate()) {
        // Correct prediction
        predictionList.add(this.request.getData());
      } else {
        // Incorrect prediction
        predictionList.add(MicroConstants.INCORRECT_PREDICTION_DATA);
      }
    }

    // Issues RPC
    try {
      switch (request.getType()) {
      case ONE_HOP:
        rpcClientStub = this.specRpcFacade.bind(serverId, MicroService.RPC_ONE_HOP);
        future = rpcClientStub.call(predictionList, rpcFactory, request.getData());
        break;
      case MULTI_HOP:
        rpcClientStub = this.specRpcFacade.bind(serverId, MicroService.RPC_MULTI_HOP);
        future = rpcClientStub.call(predictionList, rpcFactory, request.getData(),
            request.getRpcHopNum(curRpcIndex) - 1);
        break;
      default:
        logger.error("Unkown RPC type = ", request.getType());
        System.exit(MicroConstants.RUNTIME_FATAL_ERROR_CODE);
      }

      ret = (String) future.getResult(); // blocking call

    } catch (MethodNotRegisteredException | IOException | InterruptedException | UserException e) {
      e.printStackTrace();
      logger.error(e.getMessage());
      System.exit(MicroConstants.RUNTIME_FATAL_ERROR_CODE);
    } catch (SpeculationFailException e1) {
      ;
    }
    
    return (String) rpcReturnValue + ret;
  }
  
  protected void doLocalComputation(long compTime) {
    try {
      if (compTime > 0) {
        Thread.sleep(compTime);
      }
    } catch (InterruptedException e) {
      e.printStackTrace();
      logger.error(e.getMessage());
      System.exit(MicroConstants.RUNTIME_FATAL_ERROR_CODE);
    }
  }

}
