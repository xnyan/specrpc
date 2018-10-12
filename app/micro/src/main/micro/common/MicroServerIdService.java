package micro.common;

public class MicroServerIdService {
  
  public static String getServerId(int rpcIndex) {
    return rpcIndex + "";
  }
  
}
