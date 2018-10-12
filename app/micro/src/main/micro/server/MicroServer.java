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

package micro.server;

import java.io.IOException;
import java.util.Properties;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import micro.common.MicroConstants;
import micro.common.MicroConstants.PREDICT_POINT;
import micro.common.MicroConstants.RPC_FRAMEWORK;
import micro.common.ServerLocationTable;
import micro.server.grpc.MicroGrpcServer;
import micro.server.specrpc.MicroServiceSpecRpcFactory;
import micro.server.tradrpc.MicroServiceTradRpcFactory;
import rpc.config.Constants;
import rpc.execption.UninitializationException;
import rpc.server.api.RpcHostObjectFactory;
import rpc.server.api.RpcServer;
import specrpc.client.api.SpecRpcClient;
import specrpc.common.RpcSignature;
import specrpc.common.RpcConfig;
import specrpc.server.api.SpecRpcServer;
import tradrpc.client.api.TradRpcClient;
import tradrpc.server.api.TradRpcServer;

public class MicroServer implements Runnable {

  private static final Logger logger = LoggerFactory.getLogger(MicroConstants.LOGGER_TYPE);

  private static Random rnd = null;
  private static long computationTimeBeforeRPC = 0;
  private static long computationTimeAfterRPC = 0;
  private static long computationTimeInToal = 0;
  private static String nextHopServerId;
  private static boolean isPredict = false;
  private static int correctPredictRate = 0;
  private static PREDICT_POINT predictPoint;

  public static Random getRandomVar() {
    return MicroServer.rnd;
  }

  public static long getComputationTimeBeforeRPC() {
    return MicroServer.computationTimeBeforeRPC;
  }

  public static long getComputationTimeAfterRPC() {
    return MicroServer.computationTimeAfterRPC;
  }

  public static long getComputationTimeInTotal() {
    return MicroServer.computationTimeInToal;
  }

  public static String getNextHopServerId() {
    return MicroServer.nextHopServerId;
  }

  public static boolean isPredict() {
    return MicroServer.isPredict;
  }

  public static int getCorrectPredictRate() {
    return MicroServer.correctPredictRate;
  }

  public static PREDICT_POINT getPredictPoint() {
    return MicroServer.predictPoint;
  }

  // MicroServer instance
  public final String serverId;
  public final String ip;
  public final int port;
  public final RPC_FRAMEWORK rpcFramework;
  private RpcServer rpcServer; // for SpecRPC and TradRPC
  private MicroGrpcServer grpcServer; // for gRPC

  public MicroServer(String id, String ip, int port, Properties serverConfig) {
    this.serverId = id;
    this.ip = ip;
    this.port = port;

    int serverNum = Integer
        .parseInt(serverConfig.getProperty(MicroConstants.WORKLOAD_SERVER_NUM_PROPERTY, MicroConstants.DEFAULT_WORKLOAD_SERVER_NUM));
    // Checks if server id is valid
    int sId = Integer.parseInt(this.serverId);
    if (sId < 1 || sId > serverNum) {
      logger.error("Invalid server id = ", this.serverId, ", expected [1 , " + serverNum + "].");
      System.exit(MicroConstants.INIT_FAIL_ERROR_CODE);
    }

    // Random seed
    long rndSeed = Long.parseLong(serverConfig.getProperty(MicroConstants.RANDOM_SEED_PROPERTY));
    MicroServer.rnd = new Random(rndSeed);

    // Computation time
    this.initServerComputationTime(serverConfig);

    MicroServer.nextHopServerId = (sId + 1) + "";
    MicroServer.isPredict = Boolean.parseBoolean(serverConfig
        .getProperty(MicroConstants.SPEC_SERVER_IS_PREDICT_PROPERTY, MicroConstants.DEFAULT_SPEC_SERVER_IS_PREDICT));
    MicroServer.correctPredictRate = Integer.parseInt(
        serverConfig.getProperty(MicroConstants.SPEC_SERVER_CORRECT_RATE_PROPERTY, MicroConstants.DEFAULT_SPEC_SERVER_CORRECT_RATE));
    MicroServer.predictPoint = PREDICT_POINT.valueOf(serverConfig
        .getProperty(MicroConstants.SPEC_SERVER_PREDICT_POINT, MicroConstants.DEFAULT_SPEC_SERVER_PREDICT_POINT)
        .toUpperCase());

    // Initializes RPC framework
    String rpcConfigFile = serverConfig.getProperty(MicroConstants.RPC_CONFIG_FILE_PROPERTY);
    String rpcFrameworkType = serverConfig.getProperty(MicroConstants.RPC_FRAMEWORK_PROPERTY,
        MicroConstants.DEFAULT_RPC_FRAMEWORK);
    this.rpcFramework = RPC_FRAMEWORK.valueOf(rpcFrameworkType.toUpperCase());

    RpcHostObjectFactory rpcHostObjectFactory = null;

    try {
      switch (this.rpcFramework) {
      case TRADRPC:
        this.rpcServer = new TradRpcServer();
        rpcHostObjectFactory = new MicroServiceTradRpcFactory();
        this.initRpcServerFramework(rpcHostObjectFactory, rpcConfigFile);
        // Initializes client-side framework
        TradRpcClient.initClient(rpcConfigFile);
        break;
      case SPECRPC:
        this.rpcServer = new SpecRpcServer();
        rpcHostObjectFactory = new MicroServiceSpecRpcFactory();
        this.initRpcServerFramework(rpcHostObjectFactory, rpcConfigFile);
        // Initializes client-side framework
        SpecRpcClient.initClient(rpcConfigFile);
        break;
      case GRPC:
        this.grpcServer = new MicroGrpcServer(this.port);
        // Initializes gRPC client-side framework
        RpcConfig rpcConfig = new RpcConfig(rpcConfigFile);
        String rpcSigFile = rpcConfig.get(Constants.RPC_HOST_SIGNATURE_FILE_PROPERTY,
            Constants.DEFAULT_RPC_HOST_SIGNATURE_FILE);
        ServerLocationTable serverLocationTable = new ServerLocationTable(rpcSigFile);
        MicroGrpcServer.initClientStub(serverLocationTable);
        break;
      default:
        logger.error("Undefined RPC framework: " + this.rpcFramework);
        System.exit(MicroConstants.INIT_FAIL_ERROR_CODE);
      }
    } catch (IOException e) {
      logger.error(e.getMessage());
      e.printStackTrace();
      System.exit(MicroConstants.INIT_FAIL_ERROR_CODE);
    }
  }

  private void initServerComputationTime(Properties serverConfig) {
    // Computation times
    MicroServer.computationTimeBeforeRPC = Long
        .parseLong(serverConfig.getProperty(MicroConstants.WORKLOAD_SERVER_COMP_TIME_BEFORE_RPC_PROPERTY,
            MicroConstants.DEFAULT_WORKLOAD_SERVER_COMP_TIME_BEFORE_RPC));
    MicroServer.computationTimeAfterRPC = Long
        .parseLong(serverConfig.getProperty(MicroConstants.WORKLOAD_SERVER_COMP_TIME_AFTER_RPC_PROPERTY,
            MicroConstants.DEFAULT_WORKLOAD_SERVER_COMP_TIME_AFTER_RPC));
    MicroServer.computationTimeInToal = MicroServer.computationTimeBeforeRPC + MicroServer.computationTimeAfterRPC;
  }

  private void initRpcServerFramework(RpcHostObjectFactory rpcHostObjectFactory, String rpcConfigFile) {
    try {
      // Initializes RPC server-side framework
      this.rpcServer.initServer(this.serverId, this.ip, this.port, rpcConfigFile);
      // RPCs to be registered
      RpcSignature[] rpcSigList = { MicroService.RPC_ONE_HOP, MicroService.RPC_MULTI_HOP };
      // Registers RPCs for clients to call
      for (RpcSignature rpcSig : rpcSigList) {
        this.rpcServer.register(rpcSig.methodName, rpcHostObjectFactory, rpcSig.returnType, rpcSig.argTypes);
      }

    } catch (IOException | UninitializationException e) {
      logger.error(e.getMessage());
      e.printStackTrace();
      System.exit(MicroConstants.INIT_FAIL_ERROR_CODE);
    }
  }

  @Override
  public void run() {
    // Starts up the RPC framework and waits for clients to call RPCs
    try {
      switch (this.rpcFramework) {
      case TRADRPC:
      case SPECRPC:
        this.rpcServer.execute();
        break;
      case GRPC:
        this.grpcServer.execute();
        break;
      default:
        logger.error("Undefined RPC framework: " + this.rpcFramework);
      }
    } catch (UninitializationException | InterruptedException | IOException e) {
      logger.error(e.getMessage());
    }
  }

}
