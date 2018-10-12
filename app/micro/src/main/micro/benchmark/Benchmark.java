package micro.benchmark;

import java.time.Duration;
import java.time.format.DateTimeParseException;
import java.util.Properties;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import micro.benchmark.workload.MicroWorkload;
import micro.client.MicroClient;
import micro.client.grpc.MicroClientGrpc;
import micro.client.request.MicroRequest;
import micro.client.specrpc.MicroClientSpecRpc;
import micro.client.tradrpc.MicroClientTradRpc;
import micro.common.MicroConstants;
import micro.common.MicroConstants.RPC_FRAMEWORK;
import specrpc.client.api.SpecRpcStatistics;

public class Benchmark {

  private static final Logger logger = LoggerFactory.getLogger(MicroConstants.LOGGER_TYPE);

  private Random rnd;
  private MicroClient microClient;
  private RPC_FRAMEWORK rpcFramework;
  private boolean isSpecStat;
  private MicroWorkload workload;
  private final long runningDuration; // in ms
  private final long benchmarkInitTime; // in ms
  private final long throughput; // # of Requests / second
  private final double durationPerRequest; // in ms

  public Benchmark(Properties config) {
    this.benchmarkInitTime = System.currentTimeMillis();
    // Running time
    String runningTime = config.getProperty(MicroConstants.BENCHMARK_RUNNING_TIME_PROPERTY,
        MicroConstants.DEFAULT_BENCHMARK_RUNNING_TIME);
    Duration duration = null;
    try {
      duration = Duration.parse(runningTime);
    } catch (DateTimeParseException e) {
      logger.error(e.getMessage());
      e.printStackTrace();
      System.exit(MicroConstants.INIT_FAIL_ERROR_CODE);
    }
    this.runningDuration = duration.toMillis();// ms
    logger.debug("Benchmark running duration: " + this.runningDuration + " ms.");

    // Target throughput
    String targetThr = config.getProperty(MicroConstants.BENCHMARK_TARGET_THROUGHPUT_PROPERTY,
        MicroConstants.DEFAULT_BENCHMARK_TARGET_THROUGHPUT);
    this.throughput = Long.parseLong(targetThr);
    if (this.throughput < 0) {
      logger.error("Invalid target throughput: " + this.throughput + ". Must be above 0.");
      System.exit(MicroConstants.INIT_FAIL_ERROR_CODE);
    }
    if (this.throughput == 0) {
      this.durationPerRequest = 0.0;
    } else {
      this.durationPerRequest = 1.0 * 1000 / this.throughput; // ms per request
    }

    // Initializes Workload
    // Random Seed
    long rndSeed = Long.parseLong(config.getProperty(MicroConstants.RANDOM_SEED_PROPERTY));
    logger.debug("Benchmark workload random seed: " + rndSeed);
    this.rnd = new Random(rndSeed);

    // Workload
    this.workload = new MicroWorkload(this.rnd, config);

    // MicroClient
    String rpcFrameworkType = config.getProperty(MicroConstants.RPC_FRAMEWORK_PROPERTY,
        MicroConstants.DEFAULT_RPC_FRAMEWORK);
    this.rpcFramework = RPC_FRAMEWORK.valueOf(rpcFrameworkType.toUpperCase());
    switch (this.rpcFramework) {
    case TRADRPC:
      this.microClient = new MicroClientTradRpc(config);
      break;
    case SPECRPC:
      this.microClient = new MicroClientSpecRpc(config);
      break;
    case GRPC:
      this.microClient = new MicroClientGrpc(config);
      break;
    default:
      logger.error("Invalid RPC framework: " + this.rpcFramework);
      System.exit(MicroConstants.INIT_FAIL_ERROR_CODE);
    }

    // Speculation Statistics
    this.isSpecStat = Boolean.parseBoolean(config.getProperty(MicroConstants.RPC_SPECRPC_STATISTICS_ENABLED_PROPERTY,
        MicroConstants.DEFAULT_RPC_SPECRPC_STATISTICS_ENABLED));
    // Overrides the one in SpecRPC config file
    SpecRpcStatistics.setIsEnabled(this.isSpecStat);
    SpecRpcStatistics.setIsCountingIncorrectPrediction(this.isSpecStat);
    SpecRpcStatistics.reset();
  }

  /**
   * Benchmark begins to execute
   */
  public void execute() {
    System.out.println("#clientId=" + this.microClient.getClientId());

    long thrTimeLeg = 0; // ms
    long requestBeginTime = 0, requestEndTime = 0;
    long execStartTime = System.currentTimeMillis();
    long execEndTime = System.currentTimeMillis();
    System.out.println("#ExecStartTime(ms) = " + execStartTime);
    // Format
    String title = "#reqId, reqBeginTime(ns), reqEndTime(ns), latency(ms)";
    if (this.isSpecStat) {
      title += ", correctPrediction, incorrectPrediction, totalPrediction";
    }
    System.out.println(title);

    while (execEndTime - execStartTime < this.runningDuration) {
      // Generates a request
      MicroRequest req = this.workload.nextRequest();

      // Resets SpecRPC statistics
      if (this.isSpecStat) {
        SpecRpcStatistics.reset();
      }

      // Begins a request
      requestBeginTime = System.nanoTime(); // request begin time

      logger.debug("Request id = " + req.id + " begins.");

      String ret = this.microClient.execRequest(req);

      requestEndTime = System.nanoTime();

      // Output results
      double latency = (requestEndTime - requestBeginTime) / 1000000.0f; // ms

      logger.debug("Request id = " + req.id + " finishes with result = " + ret);

      String metrics = req.id + ", " + // id
          requestBeginTime + ", " + // reqeust begin time (ns)
          requestEndTime + ", " + // request end time (ns)
          latency;// request completion time (ms)
      if (this.isSpecStat) {
        metrics += ", " + SpecRpcStatistics.getCorrectPredictionNumber() + ", "
            + SpecRpcStatistics.getIncorrectPredictionNumber() + ", " + SpecRpcStatistics.getTotalPredictionNumber();
      }
      System.out.println(metrics);

      /**
       * Trying to maintain the target throughput.
       * 
       * If can not achieve the target throughput, sending request back-to-back
       */
      if (latency < this.durationPerRequest) {
        long expectWaitTime = (long) (this.durationPerRequest - latency);
        // Compansate
        if (thrTimeLeg <= expectWaitTime) {
          expectWaitTime -= thrTimeLeg;
          thrTimeLeg = 0;
        } else {
          thrTimeLeg -= expectWaitTime;
          expectWaitTime = 0;
        }
        if (expectWaitTime > 0) {
          try {
            Thread.sleep(expectWaitTime);
          } catch (InterruptedException e) {
            logger.error(e.getMessage());
            e.printStackTrace();
            System.exit(MicroConstants.RUNTIME_FATAL_ERROR_CODE);
          }
        }
      } else {
        long leg = (long) (latency - this.durationPerRequest);
        if (thrTimeLeg + leg < Long.MAX_VALUE) {
          // Prevents overflow
          thrTimeLeg += leg;
        }
      }

      execEndTime = System.currentTimeMillis();
    }

    System.out.println("#ExecEndTime (ms) = " + execEndTime + ", Actual Execution Duration (ms) = "
        + (execEndTime - execStartTime) + ", Benchmark Duration (ms) = " + (execEndTime - this.benchmarkInitTime)
        + ", Throughput Time Leg(ms) = " + thrTimeLeg);
  }

  /**
   * Cleans up
   */
  public void shutdown() {
    try {
      /*
       * Waits for a while for the client (i.e. RPC communication lib) to receive the
       * extra reply from servers, that is, the extra-to-quorum reply for PaxosCommit.
       * 
       * Otherwise, an InterruptedException regarding the communication channel may be
       * thrown by the thread that waits on the reply.
       * 
       * This is not related to the correctness. Just a slow shutdown to avoid the
       * exception.
       * 
       * TODO Handles the exception at the waiting thread.
       */
      Thread.sleep(1500);// TODO not hard coded.
    } catch (InterruptedException e) {
      logger.error(e.getMessage());
      e.printStackTrace();
      System.exit(MicroConstants.RUNTIME_FATAL_ERROR_CODE);
    }
    this.microClient.shutdown();
  }
}
