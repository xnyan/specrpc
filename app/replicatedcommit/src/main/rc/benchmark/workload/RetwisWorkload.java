package rc.benchmark.workload;

import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rc.client.txn.ClientTxnOperation;
import rc.client.txn.ReadOperation;
import rc.client.txn.WriteOperation;
import rc.common.RcConstants;
import utils.ZipfGenerator;

/**
 * RetwisWorkload is a clone of TAPIR's C++-version Retwis workload in Java.
 * 
 * Note: TAPIR's Retwis workload (Sep. 2017) has a bug in the binary search implementation
 * for the Zipf distribution. 
 * 
 * RetwisWorkload does not use the Zipf distribution in TAPIR's Retwis workload.
 * 
 */
public class RetwisWorkload extends Workload {

  private static final Logger logger = LoggerFactory.getLogger(RcConstants.LOGGER_TYPE);

  private final int addUserPortionCap; // = addUser portion
  private final int followPortionCap; // = addUser + follow portion
  private final int postPortionCap; // = addUser + follow + post portion
  private final int loadPortionCap; // = addUser + follow + post + load portion = 100
  
  public RetwisWorkload(
      int addUserP, // add user portion [0, 100]
      int followP, // follow/unfollow portion [0, 100]
      int postP, // post tweet portion [0, 100]
      int loadP, // load timeline portion [0, 100]
      String[] keyList,
      ZipfGenerator zipfGenerator,
      long randomSeed) {
    
    super(keyList, zipfGenerator, randomSeed);
    
    this.addUserPortionCap = addUserP;
    this.followPortionCap = this.addUserPortionCap + followP;
    this.postPortionCap = this.followPortionCap + postP;
    this.loadPortionCap = this.postPortionCap + loadP;
    
    if (this.loadPortionCap != 100) {
      logger.error("Invalid addUser, follow/unfollow, post-tweet, and load-timeline portion set up for Retwis.");
      System.exit(RcConstants.INIT_FAIL_ERROR_CODE);
    }
  }
  
  @Override
  public ClientTxnOperation[] nextTxn() {
    ArrayList<ClientTxnOperation> txnOpList = new ArrayList<ClientTxnOperation>();
    int percentage = this.rnd.nextInt(RcConstants.PERCENTAGE); // [0, 100]
    if (percentage < this.addUserPortionCap) {
      // add user: 1 reads, 3 writes
      for (int i = 0; i < 3; i++) {
        String key = this.genKey();
        if (i == 0) {
          // read
          txnOpList.add(new ReadOperation(key));
        }
        // write
        txnOpList.add(new WriteOperation(key, this.genVal()));
      }
    } else if (percentage < this.followPortionCap) {
      // follow / unfollow: 2 reads, 2 writes
      for (int i = 0; i < 2; i ++) {
        String key = this.genKey();
        // read
        txnOpList.add(new ReadOperation(key));
        // write
        txnOpList.add(new WriteOperation(key, this.genVal()));
      }
    } else if (percentage < this.postPortionCap) {
      // post tweet: 3 reads, 5 writes
      for (int i = 0; i < 5; i ++) {
        String key = this.genKey();
        if (i < 3) {
          // read
          txnOpList.add(new ReadOperation(key));
        }
        // write
        txnOpList.add(new WriteOperation(key, this.genVal()));
      }
      
    } else if (percentage < this.loadPortionCap) {
      // load timeline [1, 10] reads
      int readNum = this.rnd.nextInt(10) + 1; //[1, 10]
      for (int i = 0; i < readNum; i++) {
        // read
        txnOpList.add(new ReadOperation(this.genKey()));
      }
    }
    
    return txnOpList.toArray(RcConstants.CLIENT_TXN_OPERATION_ARRAY_TYPE_HELPER);
  }

}
