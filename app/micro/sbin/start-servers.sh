#!/bin/bash

sbin="`dirname "$0"`"
sbin="`cd "$sbin"; pwd`"
source $sbin/func.sh

deploy_config=$1
shift
server_list_file=$1
shift
RPC=$1

if [ -z $deploy_config ] || [ -z $server_list_file ] || [ -z $RPC ]; then
  echo "Usage: <deploy_config> <server-list> <rpc> [#cpu, such as fff]"; exit 1
fi
if [ ! -f $deploy_config ] || [ ! -f $server_list_file ]; then
  echo "File does not exist, deploy config file or server list file."; exit 1
fi

shift
cpu_num=$1

if [ -n "$cpu_num" ]; then
  pin_cpu="-z $cpu_num"
fi

#RPC framework type: t for TradRPC, s for SpecRPC, g for gRPC
RPC="-$RPC"
echo "RPC Option: $RPC"

read_config $server_list_file
source $deploy_config

#Execution directory
eval EXEC_DIR=${DEPLOY_DST_DIR}
echo "Execution directory: $EXEC_DIR"
#Micro Benchmark configuration file
MICRO_CONF=${DEPLOY_MICRO_CONFIG_FILE}
echo "Micro benchmark Configuration File: $MICRO_CONF"

#Run servers
for config in "${CONFIG_LIST[@]}";
do
  #Parses each line
  config=($config)
  echo "Starting server with config: ${config[@]}"
  server_id="${config[0]}"
  $sbin/server.sh \
    -e "$EXEC_DIR" \
    -d ${config[0]} -i ${config[1]} \
    -p ${config[2]} -c $MICRO_CONF $RPC -b -l server-${server_id}.log $pin_cpu
  #Allows each server to register its RPC signatures
  sleep 1
  echo "Started server with config: ${config[@]}"
done

