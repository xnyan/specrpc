#!/bin/bash

sbin="`dirname "$0"`"
sbin="`cd "$sbin"; pwd`"
source $sbin/func.sh

server_list_file=$1
if [ -z $server_list_file ]; then
  echo "Usage: <server-list> [#cpu, such as fff]"
  exit 1
fi
shift
cpu_num=$1

if [ -n "$cpu_num" ]; then
  pin_cpu="-z $cpu_num"
fi

#if [ -z $SPECRPC_HOME ]; then
#  echo "Can not find \$SPECRPC_HOME. Tring to configure automatically..."
#  . "$sbin/../../../config.sh"
#fi
#RC_HOME="$SPECRPC_HOME/app/replicatedcommit"

read_config $server_list_file
count=0
for config in "${CONFIG_LIST[@]}";
do
  count=$((count+1))
  #Parses each line
  config=($config)
  if [ "$count" == "1" ];then
    #Replicated Commit configuration file
    RC_CONF=${config[1]}
    echo "ReplicatedCommit Configuration File: $RC_CONF"
  elif [ "$count" == "2" ]; then
    #RPC framework type: t for TradRPC, s for SpecRPC, g for gRPC
    RPC=${config[1]}
    echo "RPC framework: $RPC"
    RPC=`echo "$RPC" | awk '{print substr($0,0,1)}'`
    RPC="-$RPC"
    echo "RPC Option: $RPC"
  elif [ "$count" == "3" ]; then
    #Server process execution directory
    eval EXEC_DIR=${config[1]}
    echo "Execution directory: $EXEC_DIR"
  else
    echo "Starting server with config: ${config[@]}"
    server_id="${config[0]}-${config[1]}"
    $sbin/server.sh \
      -e "$EXEC_DIR" \
      -d ${config[0]} -r ${config[1]} -i ${config[2]} \
      -p ${config[3]} -c $RC_CONF $RPC -b -l server-${server_id}.log $pin_cpu
    #Allows each server to register its RPC signatures
    sleep 1
    echo "Started server with config: ${config[@]}"
  fi
done


