package micro.server.specrpc;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import Waterloo.MultiSocket.exception.ConnectionCloseException;
import Waterloo.MultiSocket.exception.MultiSocketValidException;
import micro.common.MicroConstants;
import micro.server.MicroServer;
import rpc.execption.NoClientStubException;
import specrpc.client.api.SpecRpcCallback;
import specrpc.common.api.SpecRpcFacade;
import specrpc.exception.SpeculationFailException;

public class MicroServerCompCallback implements SpecRpcCallback {

  private static final Logger logger = LoggerFactory.getLogger(MicroConstants.LOGGER_TYPE);

  private SpecRpcFacade specRpcFacade;

  public MicroServerCompCallback() {

  }

  @Override
  public void bind(SpecRpcFacade specRPCFacade) {
    this.specRpcFacade = specRPCFacade;
  }

  @Override
  public Object run(Object rpcReturnValue) throws SpeculationFailException, InterruptedException {
    
    if (MicroServer.isPredict() && MicroServer.getPredictPoint() == MicroConstants.PREDICT_POINT.AFTER_RPC) {
      // Does prediction
      try {
        int correctP = MicroServer.getRandomVar().nextInt(MicroConstants.PERCENTAGE); // [0,99]
        if (correctP < MicroServer.getCorrectPredictRate()) {
          // Correct prediction
          this.specRpcFacade.specReturn(rpcReturnValue);
        } else {
          // Incorrect prediction
          this.specRpcFacade.specReturn(MicroConstants.INCORRECT_PREDICTION_DATA);
        }
      } catch (NoClientStubException | IOException | MultiSocketValidException | ConnectionCloseException e) {
        e.printStackTrace();
        logger.error(e.getMessage());
        System.exit(MicroConstants.RUNTIME_FATAL_ERROR_CODE);
      }
    }
    
    long time = MicroServer.getComputationTimeAfterRPC();
    if (time > 0) {
      try {
        Thread.sleep(time);
      } catch (InterruptedException e) {
        e.printStackTrace();
        logger.error(e.getMessage());
        System.exit(MicroConstants.RUNTIME_FATAL_ERROR_CODE);
      }
    }
    return rpcReturnValue;
  }

}
