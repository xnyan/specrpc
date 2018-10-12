#!/bin/bash

source ../../sbin/func.sh
source ./deploy.conf

client_list="$DEPLOY_RC_DIR/$DEPLOY_CLIENT_LIST_FILE"
echo "client list: $client_list"

read_config $client_list

log_num=3

check_client() {
  echo "==== Begin to check client $client ===="
  echo ""
  
  ssh $client "cd $dir; ls; ps -ef | grep java; df -h"
  echo ""
  
  echo "==== Finish checking client $client ===="
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
    client=${config[3]}
    check_client
  fi 
done
