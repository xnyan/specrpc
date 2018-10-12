package rc.benchmark;

import java.time.Duration;
import java.time.format.DateTimeParseException;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rc.benchmark.workload.RetwisWorkload;
import rc.benchmark.workload.Workload;
import rc.benchmark.workload.YcsbTWorkload;
import rc.client.RcClient;
import rc.client.RcClientTxn;
import rc.client.txn.ClientTxnOperation;
import rc.client.txn.ReadFailedException;
import rc.client.txn.TxnException;
import rc.common.RcConstants;
import rc.common.Utils;
import specrpc.client.api.SpecRpcStatistics;
import utils.ZipfDynamicBinarySearchImpl;
import utils.ZipfGenerator;
import rc.common.RcConstants.DATA_DISTRIBUTION;
import rc.common.RcConstants.WORKLOAD_TYPE;

public class Benchmark {

  private static final Logger logger = LoggerFactory.getLogger(RcConstants.LOGGER_TYPE);

  private RcClient rcClient;
  private boolean isSpecStat;
  private Workload workload;
  private final long runningDuration; // in ms
  private final long benchmarkInitTime; // in ms
  private final long throughput; // # of Txns / second
  private final double durationPerTxn; // in ms

  public Benchmark(Properties config) {
    this.benchmarkInitTime = System.currentTimeMillis();
    // Running time
    String runningTime = config.getProperty(RcConstants.BENCHMARK_RUNNING_TIME_PROPERTY,
        RcConstants.DEFAULT_BENCHMARK_RUNNING_TIME);
    Duration duration = null;
    try {
      duration = Duration.parse(runningTime);
    } catch (DateTimeParseException e) {
      logger.error(e.getMessage());
      e.printStackTrace();
      System.exit(RcConstants.INIT_FAIL_ERROR_CODE);
    }
    this.runningDuration = duration.toMillis();// ms
    logger.debug("Benchmark running duration: " + this.runningDuration + " ms.");
    
    // Target throughput
    String targetThr = config.getProperty(RcConstants.BENCHMARK_TARGET_THROUGHPUT_PROPERTY,
        RcConstants.DEFAULT_BENCHMARK_TARGET_THROUGHPUT);
    this.throughput = Long.parseLong(targetThr);
    if (this.throughput < 0) {
      logger.error("Invalid target throughput: " + this.throughput + ". Must be above 0.");
      System.exit(RcConstants.INIT_FAIL_ERROR_CODE);
    }
    if (this.throughput == 0) {
      this.durationPerTxn = 0.0;
    } else {
      this.durationPerTxn = 1.0 * 1000 / this.throughput; // ms per txn
    }

    // Initializes Workload
    // Random Seed
    long rndSeed = Long.parseLong(
        config.getProperty(RcConstants.WORKLOAD_RANDOM_SEED_PROPERTY, RcConstants.DEFAULT_WORKLOAD_RANDOM_SEED));
    if (rndSeed == 0) {
      // Dynamic random seed
      logger.debug("Benchmark workload random seed is set as dynamic.");
      rndSeed = System.nanoTime();
    } else {
      // Clients with the same configuration file must have different random seeds
      String cLibId = config.getProperty(RcConstants.CLIENT_LIB_ID_PROPERTY); // Client ID is unique
      if (cLibId == null) {
        logger.error("Missing client id. Initialization benchmark/workload failed.");
        System.exit(RcConstants.INIT_FAIL_ERROR_CODE);
      }
      rndSeed += cLibId.hashCode();
    }
    logger.debug("Benchmark workload random seed: " + rndSeed);

    // Keys
    String keyFile = config.getProperty(RcConstants.DATA_KEY_FILE_PROPERTY);
    String[] keyList = Utils.readKeys(keyFile);
    logger.debug("Loaded " + keyList.length + " keys from file: " + keyFile);

    // Distribution
    String distTypeStr = config.getProperty(RcConstants.WORKLOAD_KEY_DISTRIBUTION_PROPERTY,
        RcConstants.DEFAULT_WORKLOAD_KEY_DISTRIBUTION);
    DATA_DISTRIBUTION distType = DATA_DISTRIBUTION.valueOf(distTypeStr.toUpperCase());
    double alpha = 0.0f;
    switch (distType) {
    case ZIPF:
      alpha = Double.parseDouble(
          config.getProperty(RcConstants.WORKLOAD_ZIPF_ALPHA_PROPERTY, RcConstants.DEFAULT_WORKLOAD_ZIPF_ALPHA));
      break;
    case UNIFORM:
      // uses Zipfian distribution with alpha = 0
      break;
    default:
      logger.error("Invalid data distribution: " + distType);
      System.exit(RcConstants.INIT_FAIL_ERROR_CODE);
    }
    ZipfGenerator zipfGenerator = new ZipfDynamicBinarySearchImpl(keyList.length, alpha, rndSeed);

    // Workload type
    String workloadTypeStr = config.getProperty(RcConstants.WORKLOAD_TYPE_PROPERTY, RcConstants.DEFAULT_WORKLOAD_TYPE);
    WORKLOAD_TYPE workloadType = WORKLOAD_TYPE.valueOf(workloadTypeStr.toUpperCase());
    switch (workloadType) {
    case YCSBT:
      // YCSB+T
      int opNum = Integer.parseInt(
          config.getProperty(RcConstants.WORKLOAD_YCSBT_OP_NUM_PROPERTY, RcConstants.DEFAULT_WORKLOAD_YCSBT_OP_NUM));
      int readP = Integer.parseInt(config.getProperty(RcConstants.WORKLOAD_YCSBT_READ_PORTION_PROPERTY,
          RcConstants.DEFAULT_WORKLOAD_YCSBT_READ_PORTION));
      int writeP = Integer.parseInt(config.getProperty(RcConstants.WORKLOAD_YCSBT_WRITE_PORTION_PROPERTY,
          RcConstants.DEFAULT_WORKLOAD_YCSBT_WRITE_PORTION));
      int rmwP = Integer.parseInt(config.getProperty(RcConstants.WORKLOAD_YCSBT_RMW_PORTION_PROPERTY,
          RcConstants.DEFAULT_WORKLOAD_YCSBT_RMW_PORTION));
      this.workload = new YcsbTWorkload(opNum, readP, writeP, rmwP, keyList, zipfGenerator, rndSeed);
      break;
    case RETWIS:
      // Retwis
      int addUserP = Integer.parseInt(config.getProperty(RcConstants.WORKLOAD_RETWIS_ADDUSER_PORTION_PROPERTY,
          RcConstants.DEFAULT_WORKLOAD_RETWIS_ADDUSER_PORTION));
      int followP = Integer.parseInt(config.getProperty(RcConstants.WORKLOAD_RETWIS_FOLLOW_PORTION_PROPERTY,
          RcConstants.DEFAULT_WORKLOAD_RETWIS_FOLLOW_PORTION));
      int postP = Integer.parseInt(config.getProperty(RcConstants.WORKLOAD_RETWIS_POST_PORTION_PROPERTY,
          RcConstants.DEFAULT_WORKLOAD_RETWIS_POST_PORTION));
      int loadP = Integer.parseInt(config.getProperty(RcConstants.WORKLOAD_RETWIS_LOAD_PORTION_PROPERTY,
          RcConstants.DEFAULT_WORKLOAD_RETWIS_LOAD_PORTION));
      this.workload = new RetwisWorkload(addUserP, followP, postP, loadP, keyList, zipfGenerator, rndSeed);
      break;
    default:
      logger.error("Invalid workload: " + workloadType);
      System.exit(RcConstants.INIT_FAIL_ERROR_CODE);
    }

    // Rc Client
    this.rcClient = new RcClient(config);
        
    // Speculation Statistics
    this.isSpecStat = Boolean.parseBoolean(config.getProperty(RcConstants.RPC_SPECRPC_STATISTICS_ENABLED_PROPERTY,
        RcConstants.DEFAULT_RPC_SPECRPC_STATISTICS_ENABLED));
    // Overrides the one in SpecRPC config file
    SpecRpcStatistics.setIsEnabled(this.isSpecStat);
    SpecRpcStatistics.setIsCountingIncorrectPrediction(this.isSpecStat);
    SpecRpcStatistics.reset();
  }

  /**
   * Benchmark begins to execute
   */
  public void execute() {
    System.out.println("#clientId=" + this.rcClient.getClientLibId());

    long thrTimeLeg = 0; // ms
    long txnBeginTime = 0, txnProcessingEndTime = 0, txnCommitEndTime = 0;
    boolean isCommitted = false, isProcessingOk = false;
    long execStartTime = System.currentTimeMillis();
    long execEndTime = System.currentTimeMillis();
    System.out.println("#ExecStartTime(ms) = " + execStartTime);
    // Format
    String title = "#txnId, txnBeginTime(ns), ProcessingEndTime(ns), CommitEndTime(ns), latency(ms), isAcquiringReadLockFailed, isCommitted";
    if (this.isSpecStat) {
      title += ", correctPrediction, incorrectPrediction, totalPrediction";
    }
    System.out.println(title);

    while (execEndTime - execStartTime < this.runningDuration) {
      // Generates txn operations
      ClientTxnOperation[] txnOpList = this.workload.nextTxn();

      // Resets SpecRPC statistics
      if (this.isSpecStat) {
        SpecRpcStatistics.reset();
      }
      
      // Begins a txn
      txnBeginTime = System.nanoTime(); // txn begin time

      RcClientTxn txn = this.rcClient.beginTxn();
      isCommitted = false;
      isProcessingOk = true;
      logger.debug("Txn id= " + txn.txnId + " begins.");

      try {
        try {
          txn.executeTxnOperations(txnOpList, 0);
        } catch (ReadFailedException e) {
          // Failed to acquire read locks
          isProcessingOk = false;
        }
        
        txnProcessingEndTime = System.nanoTime(); // txn processing end time
        if (logger.isDebugEnabled()) {
          logger.debug("Txn id= " + txn.txnId + " finishes txn processing. Processing Latency= " + (txnProcessingEndTime - txnBeginTime) / 1000000.0f + "ms");
        }

        // Commits or aborts the txn
        if (isProcessingOk) {
          logger.debug("Txn id= " + txn.txnId + " tries to commit.");
          isCommitted = txn.commit();
        } else {
          logger.debug("Txn id= " + txn.txnId + "tries to abort.");
          txn.abort();
        }
        txnCommitEndTime = System.nanoTime(); // txn commit end time
        if (logger.isDebugEnabled()) {
          logger.debug("Txn id= " + txn.txnId + " finishes txn commit. Commit Latency= " + (txnCommitEndTime - txnProcessingEndTime) / 1000000.0f + "ms");
        }

      } catch (TxnException e) {
        // Txn execution failed
        logger.error("TxnException for txn id = " + txn.txnId);
        logger.error(e.getMessage());
        e.printStackTrace();
        System.exit(RcConstants.RUNTIME_FATAL_ERROR_CODE);
      }

      // Output results
      double latency = (txnCommitEndTime - txnBeginTime) / 1000000.0f; // ms
      String metrics = 
          txn.txnId + ", " + // txnId
          txnBeginTime + ", " + // txn begin time (ns)
          txnProcessingEndTime + ", " + // txn processing (read and write) end time (ns)
          txnCommitEndTime + ", " + // txn commit end time (ns)
          latency + ", " + // txn completion time (ms)
          isProcessingOk + ", " + // is read locks acquired
          isCommitted; // is txn committed
      if (this.isSpecStat) {
        metrics += ", " + SpecRpcStatistics.getCorrectPredictionNumber() +
            ", " + SpecRpcStatistics.getIncorrectPredictionNumber() +
            ", " + SpecRpcStatistics.getTotalPredictionNumber();
      }
      System.out.println(metrics);
      
      /**
       * Trying to maintain the target throughput.
       * 
       * If can not achieve the target throughput, sending txns back-to-back
       */
      if (latency < this.durationPerTxn) {
        long expectWaitTime = (long)(this.durationPerTxn - latency);
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
            System.exit(RcConstants.RUNTIME_FATAL_ERROR_CODE);
          }
        }
      } else {
        long leg = (long) (latency - this.durationPerTxn);
        if (thrTimeLeg + leg < Long.MAX_VALUE) {
          // Prevents overflow
          thrTimeLeg += leg;
        }
      }

      execEndTime = System.currentTimeMillis();
    }

    System.out.println(
        "#ExecEndTime (ms) = " + execEndTime + 
        ", Actual Txn Execution Duration (ms) = " + (execEndTime - execStartTime) + 
        ", Benchmark Duration (ms) = " + (execEndTime - this.benchmarkInitTime) +
        ", Throughput Time Leg(ms) = " + thrTimeLeg);
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
       * Otherwise, an InterruptedException regarding the communication channel may
       * be thrown by the thread that waits on the reply.
       * 
       * This is not related to the correctness. Just a slow shutdown to avoid the exception.
       * 
       * TODO Handles the exception at the waiting thread.
       */
      Thread.sleep(1500);// TODO not hard coded.
    } catch (InterruptedException e) {
      logger.error(e.getMessage());
      e.printStackTrace();
      System.exit(RcConstants.RUNTIME_FATAL_ERROR_CODE);
    }
    this.rcClient.shutdown();
  }
}
