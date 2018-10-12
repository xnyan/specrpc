#!/bin/bash

source ../../sbin/func.sh
source ./deploy.conf

server_list="$DEPLOY_RC_DIR/$DEPLOY_SERVER_LIST_FILE"
echo "server list: $server_list"

read_config $server_list

log_num=3

check_server() {
  echo "==== Begin to check server $server ===="
  echo ""
  
  ssh $server "cd $dir; ls; ps -ef | grep java; df -h"
  echo ""
  
  echo "==== Finish checking server $server ===="
  echo ""
}

count=0
for config in "${CONFIG_LIST[@]}";
do
  count=$((count+1))
  #Parses each line
  config=($config)
  if [ "$count" == "3" ]; then
    dir=${config[1]}
  fi
  if (( count < 4 )); then
    continue
  else
    server=${config[2]}
    check_server
  fi 
done
