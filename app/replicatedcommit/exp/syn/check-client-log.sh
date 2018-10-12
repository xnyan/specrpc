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
  
  echo "==== Last $log_num Lines of Logs on $client ===="
  ssh $client "cd $dir; tail -n $log_num client-*-*-?.log"
  echo ""
  
  echo "==== Errors on $client ===="
  ssh $client "cd $dir; cat client-*-*-?.log | grep ERROR"
  echo ""
  
  echo "==== Exceptions on $client ===="
  ssh $client "cd $dir; cat client-*-*-?.log | grep Exception"
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
