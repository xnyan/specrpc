package micro.server.grpc;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import micro.common.MicroConstants;
import micro.common.ServerLocationTable;
import micro.common.grpc.ClientStubGrpc;

public class MicroGrpcServer {
  
  private static final Logger logger = LoggerFactory.getLogger(MicroConstants.LOGGER_TYPE);
  
  public static ClientStubGrpc grpcClientStub = null;
  public static void initClientStub(ServerLocationTable serverLocationTable) {
    MicroGrpcServer.grpcClientStub = new ClientStubGrpc(serverLocationTable);
  }

  private final Server grpcServer;

  public MicroGrpcServer(int port) {
    this.grpcServer = ServerBuilder.forPort(port).addService(new MicroServiceGrpcImpl()).build();
  }
  
  public void execute() throws IOException {
    this.grpcServer.start();
    try {
      this.grpcServer.awaitTermination();
    } catch (InterruptedException e) {
      e.printStackTrace();
      logger.error(e.getMessage());
      System.exit(MicroConstants.RUNTIME_FATAL_ERROR_CODE);
    }
  }
}
