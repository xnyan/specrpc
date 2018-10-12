package micro.client.specrpc;

import micro.client.request.MicroRequest;
import specrpc.client.api.SpecRpcCallback;
import specrpc.client.api.SpecRpcCallbackFactory;

public class MicroClientSpecRpcCallbackFactory implements SpecRpcCallbackFactory {

  private MicroRequest request;
  private int curRpcIndex; // [2, n]
  
  public MicroClientSpecRpcCallbackFactory(MicroRequest req, int curRpcIdx) {
    this.request = req;
    this.curRpcIndex = curRpcIdx;
  }
  
  @Override
  public SpecRpcCallback createCallback() {
    return new MicroClientSpecRpcCallback(this.request, this.curRpcIndex);
  }

}
