#!/bin/bash
sbin="`dirname "$0"`"
sbin="`cd "$sbin"; pwd`"
source $sbin/func.sh

deploy_config=$1
shift
client_list_file=$1
shift
RPC=$1

if [ -z $deploy_config ] || [ -z $client_list_file ] || [ -z $RPC ]; then
  echo "Usage: <deploy-config> <client-list> <t|s|g>"; exit 1
fi
if [ ! -f $deploy_config ] || [ ! -f $client_list_file ]; then
  echo "File does not exist, deploy config file or client list file."; exit 1
fi

#RPC framework type: t for TradRPC, s for SpecRPC, g for gRPC
RPC="-$RPC"
echo "RPC Option: $RPC"

read_config $client_list_file
source $deploy_config

#Execution directory
eval EXEC_DIR=${DEPLOY_DST_DIR}
echo "Execution directory: $EXEC_DIR"
#Micro Benchmark configuration file
MICRO_CONF=${DEPLOY_MICRO_CONFIG_FILE}
echo "Micro benchmark Configuration File: $MICRO_CONF"

#Run clients
for config in "${CONFIG_LIST[@]}";
do
  #Parses each line
  config=($config)
  echo "Starting clients with config: ${config[@]}"
  $sbin/client.sh \
    -e "$EXEC_DIR" \
    -m ${config[0]} -n ${config[1]} \
    -a ${config[2]} \
    -c $MICRO_CONF $RPC \
    -b &
  sleep 0.015
  #sleep 0.050
  echo "Started clients with config: ${config[@]}"
done

wait
