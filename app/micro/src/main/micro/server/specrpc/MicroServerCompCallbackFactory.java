package micro.server.specrpc;

import specrpc.client.api.SpecRpcCallback;
import specrpc.client.api.SpecRpcCallbackFactory;

public class MicroServerCompCallbackFactory implements SpecRpcCallbackFactory {
  
  public MicroServerCompCallbackFactory() {
    
  }
  
  @Override
  public SpecRpcCallback createCallback() {
    return new MicroServerCompCallback();
  }

}
