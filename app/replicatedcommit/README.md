A prototype implementation for the Replicated Commit (RC) protocol [1]. This
prototype implements the RC protocol with three RPC frameworks, gRPC, TradRPC,
and SpecRPC. The prototype does not implement the fault tolerance features in
RC.

Check the exp-sample.sh under the exp/syn folder to see how to run RC by using
the the scripts in sbin.

References

[1] Low-Latency Multi-Datacenter Databases using Replicated Commit, VLDB, 2013
