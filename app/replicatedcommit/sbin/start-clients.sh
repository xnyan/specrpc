#!/bin/bash

sbin="`dirname "$0"`"
sbin="`cd "$sbin"; pwd`"
source $sbin/func.sh

client_list_file=$1
if [ -z $client_list_file ]; then
  echo "Usage: <client-list>"
  exit 1
fi

#if [ -z $SPECRPC_HOME ]; then
#  echo "Can not find \$SPECRPC_HOME. Tring to configure automatically..."
#  . "$sbin/../../../config.sh"
#fi
#RC_HOME="$SPECRPC_HOME/app/replicatedcommit"

read_config $client_list_file
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
    echo "Starting clients with config: ${config[@]}"
    $sbin/client.sh \
      -e "$EXEC_DIR" \
      -d ${config[0]} -m ${config[1]} -n ${config[2]} \
      -a ${config[3]} -p ${config[4]} \
      -c $RC_CONF $RPC \
      -b &
    sleep 0.015
    #sleep 0.050
    #Allows each SpecRPC client to register its RPC signatures
    #sleep 1
    echo "Started clients with config: ${config[@]}"
  fi
done

wait
