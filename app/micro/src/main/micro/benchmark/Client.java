package micro.benchmark;

import java.util.Properties;

public class Client {

  public static void main(String args[]) {
    // Parses arguments
    Properties config = ClientArgs.parseArgs(args);

    // Executes a benchmark instance
    Benchmark benchmark = new Benchmark(config);
    benchmark.execute();
    benchmark.shutdown();
  }

}
