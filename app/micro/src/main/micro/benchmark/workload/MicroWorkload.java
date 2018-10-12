package micro.benchmark.workload;

import java.util.Properties;
import java.util.Random;

import micro.client.request.MicroRequest;
import micro.common.MicroConstants;
import micro.common.Utils;

public class MicroWorkload {

  protected Random rnd;
  protected final MicroRequest.MICRO_REQUEST_TYPE type;
  protected final int rpcNum;
  protected final int rpcHopNum;
  protected final int requestDataLength; // bytes
  protected final long localCompTime; // ms
  private int reqCount;

  public MicroWorkload(Random rnd, Properties config) {
    this.rnd = rnd;
    this.type = MicroRequest.MICRO_REQUEST_TYPE.valueOf(
        config.getProperty(MicroConstants.WORKLOAD_TYPE_PROPERTY, MicroConstants.DEFAULT_WORKLOAD_TYPE).toUpperCase());
    this.rpcNum = Integer.parseInt(config.getProperty(MicroConstants.WORKLOAD_REQUEST_RPC_NUM_PROPERTY,
        MicroConstants.DEFAULT_WORKLOAD_REQUEST_RPC_NUM));
    this.rpcHopNum = Integer.parseInt(config.getProperty(MicroConstants.WORKLOAD_REQUEST_RPC_HOP_NUM_PROPERTY,
        MicroConstants.DEFAULT_WORKLOAD_REQUEST_RPC_HOP_NUM));
    this.requestDataLength = Integer.parseInt(config.getProperty(MicroConstants.WORKLOAD_REQUEST_DATA_LENGTH_PROPERTY,
        MicroConstants.DEFAULT_WORKLOAD_REQUEST_RPC_DATA_SIZE));
    this.localCompTime = Long
        .parseLong(config.getProperty(MicroConstants.WORKLOAD_CLIENT_LOCAL_COMP_TIME_AFTER_RPC_PROPERTY,
            MicroConstants.DEFAULT_WORKLOAD_CLIENT_LOCAL_COMP_TIME_AFTER_RPC));
    this.reqCount = 0;
  }
  
  private synchronized int getCount() {
    this.reqCount ++;
    return this.reqCount;
  }

  public MicroRequest nextRequest() {
    String data = this.genRequestData();
    MicroRequest req = new MicroRequest(this.getCount() + "", this.type, data);
    int rpcNum = this.rpcNum;
    for (int i = 0; i < rpcNum; i++) {
      req.addRpc(this.genRpcHopNum(), this.genLocalCompTime());
    }
    return req;
  }

  public int genRpcNum() {
    return this.rpcNum;
  }

  public int genRpcHopNum() {
    return this.rpcHopNum;
  }

  public String genRequestData() {
    return Utils.genString(this.rnd, this.requestDataLength);
  }

  public long genLocalCompTime() {
    return this.localCompTime;
  }

}
