# SpecRPC

SpecRPC is an RPC framework that facilitates applications to perform
speculative RPCs in order to reduce their latencies.
It aims to reduce the barriers of using speculation in distributed applications
to overlap the executions of multiple RPCs and local operations that have
dependencies.
SpecRPC introduces a design pattern that will simplify the implementation of
such speculative executions.
A paper about SpecRPC has been published in
[Middleware'18](http://2018.middleware-conference.org/).

# Repo Information

This repo includes the source code of SpecRPC's prototype that is implemented
in Java.
The "build.sh" script in the root directory will build SpecRPC as a jar lib,
which will be easy to deploy and test in an application.

In the "/core" directory, there are a set of tests to examine the correctness
of the SpecRPC framework on performing speculative execution.
Any further changes made to the framework should pass the tests before being
applied.

In the "/app" directory, there is an example showing the basic usage of
SpecRPC. 
Besides that, there is a microbenchmark comparing the performance of SpecRPC
with gRPC and TradRPC. 
TradRPC is an RPC framework that shares the code of SpecRPC, but there is no
speculation support.

The repo also includes an implementation of [Replicated
Commit](http://www.vldb.org/pvldb/vol6/p661-mahmoud.pdf) by using SpecRPC,
TradRPC, and gRPC, respectively. 
Replicated Commit is a transaction commit protocol for geo-replicated database
systems.
The implemented Replicated Commit can be configured to run by using an
in-memory key-value store or
[BerkeleyDB](https://www.oracle.com/database/berkeley-db/) as the back-end data
storage. 
In either case, the system will asynchronously persist the transaction log to
disks.
The implementation does not consist of the fault-tolerance features in
Replicated Commit.

Besides the above applications showing the usage of SpecRPC, a tutorial for
SpecRPC is under construction and will be coming soon.

# Installation

## Requirements 

openjdk-8+

maven-3.5+

## Compilation

git clone https://github.com/xnyan/specrpc.git ./

export SPECRPC_HOME="The Path to SpecRPC directory" or use the "config.sh" script under ${SPECRPC_HOME} 

run the "${SPECRPC_HOME}/build.sh" script to build the SpecRPC lib and the applications in this repo.

The SpecRPC lib will be generated as a file like "specrpc-core-0.1-jar-with-dependencies.jar"
in the directory of $SPECRPC_HOME/core/target/

# Tutorial

Under construction.


# SpecRPC's Design Pattern and Features

SpecRPC allows an RPC to take a Callback.
Without speculation, once the RPC completes, the Callback will execute with the
RPC's return value.
To use speculation, an application just simply passes its prediction for the RPC's
return value into SpecRPC.
While executing the RPC, SpecRPC will use the prediction to speculatively
execute the Callback.
When the RPC completes, SpecRPC knows whether the the prediction is correct.
If the prediction is correct, the Callback and the RPC execute in parallel,
which will reduce the latency compared to the sequential execution.

## Multiple Concurrent Speculations for an Execution

An application can make multiple predictions for an RPC's return value.
For each prediction, SpecRPC will speculatively execute a Callback.
SpecRPC will only return the result of the Callback that executes with the
correct prediction.
SpecRPC will discard the computation in Callbacks that are based on incorrect
predictions.
If there is no correct predictions, SpecRPC will create a new Callback that
executes with the RPC's return value.
In this case, the application would have almost the same latency compared to
the sequential execution of the RPC and the Callback.

In SpecRPC, both of an RPC client and server can make predictions of an RPC's
result.
In the former case, the RPC client passes its predictions into the SpecRPC
framework at the time of invoking the RPC.
In the latter case, the RPC server can return its predictions to the RPC client
in the middle of the RPC's execution.
This provides more opportunities for an application to make predictions in
order to perform speculative executions.

## Performing Speculative RPCs

SpecRPC also allows an application to perform RPCs within a speculative
execution, where these RPCs will be speculative too.
The application can also make predictions for the results of speculative RPCs
in order to perform more speculative execution.
By using SpecRPC, an application does not need to track the dependencies between
speculative and non-speculative operations.
Instead, SpecRPC will track the dependencies and discard incorrect speculations.

## Preventing Side-Effects

SpecRPC recommends that an application encapsulates state changes within
Callbacks and RPCs.
This will allow the SpecRPC framework to discard a speculative Callback or RPC
without causing side-effects.
However, in some cases, an application may not be able to do so. 
Therefore, SpecRPC provides additional support for applications to prevent
side-effects that may be caused by incorrect speculation.
One support is to allow an application to hold its current speculative
execution before generating potential side-effects until the SpecRPC framework
can determine if the speculation is correct or not.
Another support is to allow an application to register an rollback function for
a speculation in the SpecRPC framework.
The framework will automatically execute the rollback function if the
speculation is incorrect. 

## Asynchronous RPC Framework

SpecRPC is also an asynchronous RPC framework.
Each RPC will immediately return a future object.
This will make an RPC client not block on the execution of the RPC and the
Callback.
The RPC client can use the future object to retrieve the Callback's result.
