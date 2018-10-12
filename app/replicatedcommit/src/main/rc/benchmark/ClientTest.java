package rc.benchmark;

import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rc.client.RcClient;
import rc.client.RcClientTxn;
import rc.client.txn.ReadFailedException;
import rc.client.txn.TxnException;
import rc.common.RcConstants;

public class ClientTest {

  private static final Logger logger = LoggerFactory.getLogger(RcConstants.LOGGER_TYPE);

  public static void main(String args[]) {
    // Parses arguments
    Properties config = ClientArgs.parseArgs(args);

    // TODO Tests with multiple clients sharing the same lib. Each client is a
    // thread.
    testBackToBackTxns(config, 100, 10);
  }

  // TODO Moves these tests to be automatic, e.g. unit tests
  /**
   * A client issues txns one by one with a given interval between any two. Each
   * txn reads and updates the same two keys. If the interval is long enough for a
   * txn to start after the previous one finishes, the expected outputs are a
   * serious updated values on the two keys with every txn committed.
   * 
   * @param config
   */
  private static void testBackToBackTxns(Properties config, long intervalInMs, int txnNum) {
    RcClient client = new RcClient(config);
    RcClientTxn txn = null;
    for (int i = 0; i < txnNum; i++) {
      txn = client.beginTxn();
      try {
        String aVal = txn.read("a");
        logger.info("Txn id = " + txn.txnId + " reads a = " + aVal);
        String bVal = txn.read("b");
        logger.info("Txn id = " + txn.txnId + " reads b = " + bVal);

        txn.write("a", (i) + "");
        logger.info("Txn id = " + txn.txnId + " writes a = " + txn.read("a"));
        txn.write("b", (i) + "");
        logger.info("Txn id = " + txn.txnId + " writes b = " + txn.read("b"));

        Boolean isCommitted = txn.commit();
        logger.info("Txn id = " + txn.txnId + " is committed = " + isCommitted);

        if (intervalInMs > 0) {
          Thread.sleep(intervalInMs);
        }

      } catch (ReadFailedException | TxnException | InterruptedException e) {
        logger.error(e.getMessage());
        continue;
      }
    }
    
    try {
      // Allows clients to accept servers' replies that are extra to a quorum before
      // closes the network.
      Thread.sleep(1000);
    } catch (InterruptedException e) {
      logger.error(e.getMessage());
    }
    client.shutdown();
  }

}
