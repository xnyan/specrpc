Running the testes defined in the ant scripts.

1. Makes sure the basedir property in both 10-server.xml and clients.xml files
points to $SPECRPC_HOME.

2. Adds gson.jar and junit.jar (in ant-test-libs) to the classpath of ant build
config.

3. Runs the 10-servers.xml via ant.

4. Runs the clients.xml via ant.

The test program starts in IterMultiServersTest.java for both client and
server. During the test, each server is an independent process, but all of the servers'
output are printed into one console.

The clients.xml starts one client process simulating that several clients
sequentially execute a couple of transactions.  In each transaction, there are
several RPCs to different servers.

This test program uses Junit.assert to check whether a transaction returns an
expected value.  If the return result does not equal to the expected one, an
AssertionError will be thrown, and the error info will be in client's console.
In this case, the client will be interrupted.

If everything goes well, the client process will shutdown automatically, but
the server processes will not.  This is used to restart the clients.xml for
continuous tests. This way also separates the debug info between clients and
servers. 

If client process does not shutdown automatically, this means that the client
blocks somewhere during execution, which indicates a potentiall bug that needs to fix.

Note: these ant scripts aim to test a distributed application, where each
process has multiple running threads. The client's behavior is not
deterministic in each run. As a result, if there is bug, this test has to
execute multiple times to reproduce the bug. If there are multiple bugs,
different exceptions may happen in multiple runs.
