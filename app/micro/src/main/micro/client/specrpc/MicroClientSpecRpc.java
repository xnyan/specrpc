package micro.client.specrpc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import micro.client.MicroClient;
import micro.client.request.MicroRequest;
import micro.common.MicroConstants;
import micro.common.MicroServerIdService;
import micro.common.specrpc.MicroSpecRandom;
import micro.server.MicroService;
import rpc.execption.MethodNotRegisteredException;
import rpc.execption.UserException;
import specrpc.client.api.SpecRpcClient;
import specrpc.client.api.SpecRpcFuture;
import specrpc.client.api.SpecRpcClientStub;
import specrpc.exception.SpeculationFailException;

public class MicroClientSpecRpc extends MicroClient {

  private static final Logger logger = LoggerFactory.getLogger(MicroConstants.LOGGER_TYPE);

  private static boolean isPredict = false;
  private static int correctPredictRate = 0;

  public static boolean isPredict() {
    return MicroClientSpecRpc.isPredict;
  }

  public static int getCorrectPredictRate() {
    return MicroClientSpecRpc.correctPredictRate;
  }

  public MicroClientSpecRpc(Properties clientConfig) {
    super(clientConfig);
    long rndSeed = Long.parseLong(clientConfig.getProperty(MicroConstants.RANDOM_SEED_PROPERTY));
    // Uses a separate random variable so that the workload has the same random
    // variable for different RPC frameworks
    MicroSpecRandom.init(rndSeed);
    MicroClientSpecRpc.isPredict = Boolean.parseBoolean(clientConfig
        .getProperty(MicroConstants.SPEC_CLIENT_IS_PREDICT_PROPERTY, MicroConstants.DEFAULT_SPEC_CLIENT_IS_PREDICT));
    MicroClientSpecRpc.correctPredictRate = Integer.parseInt(clientConfig.getProperty(
        MicroConstants.SPEC_CLIENT_CORRECT_RATE_PROPERTY, MicroConstants.DEFAULT_SPEC_CLIENT_CORRECT_RATE));
  }

  @Override
  public void initRpcFramework(Properties config) {
    try {
      SpecRpcClient.initClient(this.rpcConfigFile);
    } catch (IOException e) {
      e.printStackTrace();
      logger.error(e.getMessage());
      System.exit(MicroConstants.INIT_FAIL_ERROR_CODE);
    }

  }

  @Override
  public String execRequest(MicroRequest request) {
    String ret = null;
    int curRpcIndex = 1;
    String serverId = MicroServerIdService.getServerId(curRpcIndex);

    SpecRpcClientStub rpcClientStub = null;
    SpecRpcFuture future = null;
    // Callbacks should move to the next RPC
    MicroClientSpecRpcCallbackFactory rpcFactory = new MicroClientSpecRpcCallbackFactory(request, curRpcIndex + 1);

    // Predictions
    ArrayList<Object> predictionList = null;
    if (MicroClientSpecRpc.isPredict()) {
      // Does prediction if configured
      predictionList = new ArrayList<Object>();
      int correctP = MicroSpecRandom.getPercent();
      if (correctP < MicroClientSpecRpc.getCorrectPredictRate()) {
        // Correct prediction
        predictionList.add(request.getData());
      } else {
        // Incorrect prediction
        predictionList.add(MicroConstants.INCORRECT_PREDICTION_DATA);
      }
    }

    // Issues RPC
    try {
      switch (request.getType()) {
      case ONE_HOP:
        rpcClientStub = SpecRpcClient.bind(serverId, MicroService.RPC_ONE_HOP);
        future = rpcClientStub.call(predictionList, rpcFactory, request.getData());
        break;
      case MULTI_HOP:
        rpcClientStub = SpecRpcClient.bind(serverId, MicroService.RPC_MULTI_HOP);
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

    return ret;
  }

  @Override
  public void shutdown() {
    SpecRpcClient.shutdown();
  }

}
