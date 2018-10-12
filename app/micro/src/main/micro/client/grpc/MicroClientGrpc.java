package micro.client.grpc;

import java.io.IOException;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import micro.client.MicroClient;
import micro.client.request.MicroRequest;
import micro.common.MicroConstants;
import micro.common.MicroLocationService;
import micro.common.MicroServerIdService;
import micro.common.ServerLocationTable;
import micro.common.grpc.ClientStubGrpc;
import micro.grpc.MultiHopRequest;
import micro.grpc.OneHopRequest;
import micro.grpc.MicroServiceGrpc.MicroServiceBlockingStub;
import rpc.config.Constants;
import specrpc.common.RpcConfig;

public class MicroClientGrpc extends MicroClient {

  private static final Logger logger = LoggerFactory
      .getLogger(MicroConstants.LOGGER_TYPE);

  private final ClientStubGrpc grpcClient;

  public MicroClientGrpc(Properties clientConfig) {
    super(clientConfig);
    this.grpcClient = new ClientStubGrpc(
        MicroLocationService.serverLocationTable);
  }

  @Override
  public void initRpcFramework(Properties config) {
    try {
      // Reuses SpecRPC's RPC signature file to locate RPC servers' ip addresses
      // and ports
      RpcConfig rpcConfig = new RpcConfig(rpcConfigFile);
      String rpcSigFile = rpcConfig.get(
          Constants.RPC_HOST_SIGNATURE_FILE_PROPERTY,
          Constants.DEFAULT_RPC_HOST_SIGNATURE_FILE);

      MicroLocationService
          .setServerLocationTable(new ServerLocationTable(rpcSigFile));

    } catch (IOException e) {
      logger.error(e.getMessage());
      e.printStackTrace();
      System.exit(MicroConstants.INIT_FAIL_ERROR_CODE);
    }
  }

  @Override
  public String execRequest(MicroRequest request) {
    String ret = null;
    int rpcNum = request.getRpcNum();

    for (int i = 1; i <= rpcNum; i++) {
      String serverId = MicroServerIdService.getServerId(i);
      String rpcRet = null;

      // Issuing an RPC
      MicroServiceBlockingStub rpcClientStub = this.grpcClient
          .getBlockingStub(serverId);
      switch (request.getType()) {
      case ONE_HOP:
        OneHopRequest oneHopReq = OneHopRequest.newBuilder()
            .setData(request.getData()).build();
        rpcRet = rpcClientStub.oneHop(oneHopReq).getData(); // blocking call
        break;
      case MULTI_HOP:
        int hopNumPerRpc = request.getRpcHopNum(i) - 1;
        MultiHopRequest multiHopReq = MultiHopRequest.newBuilder()
            .setData(request.getData()).setHopNum(hopNumPerRpc).build();
        rpcRet = rpcClientStub.multiHop(multiHopReq).getData(); // blocking call
        break;
      default:
        logger.error("Unkown RPC type = ", request.getType());
        System.exit(MicroConstants.RUNTIME_FATAL_ERROR_CODE);
      }

      // Combines RPC return values
      if (ret == null) {
        ret = rpcRet;
      } else {
        ret += rpcRet;
      }

      // Does local computation
      this.doLocalComputation(request.getLocalCompTime(i));

    }

    return null;
  }

  @Override
  public void shutdown() {
    // Does nothing
  }

}
