package micro.common.specrpc;

import java.util.Random;

import micro.common.MicroConstants;

public class MicroSpecRandom {

  private static Random rnd = null;
  
  public static void init(long rndSeed) {
    rnd = new Random(rndSeed);
  }
  
  public static Random getRnd() {
    return rnd;
  }
  
  /**
   * Returns [0, 99]
   * @return
   */
  public static int getPercent() {
    return rnd.nextInt(MicroConstants.PERCENTAGE);
  }
}
