#!/bin/bash

source ../../sbin/func.sh
source ./deploy.conf

client_list="$DEPLOY_RC_DIR/$DEPLOY_CLIENT_LIST_FILE"
echo "client list: $client_list"

read_config $client_list

if [ -z $2 ]; then
  echo "Usage: <output path> <output dir>"; exit 1
fi

#output_path="./exp-data/janus-dc/retwis"
output_path=$1
shift
output_dir=$1
log_num=3

copy_data() {
  echo "==== Begin to copy data from client $client ===="
  echo ""

  dst="$output_path/$rpc_type/$output_dir"
  mkdir -p $output_path/$rpc_type/$output_dir
  scp $client:$dir/client-*-*-*.log $dst/
  cp $rc_config $dst/
  rm $dst/*.pid.log
  
  echo "==== Finish copying data from client $client to $dst ===="
  echo ""
}

count=0
for config in "${CONFIG_LIST[@]}";
do
  count=$((count+1))
  #Parses each line
  config=($config)
  if [ "$count" == "1" ]; then
    rc_config=${config[1]}
  elif [ "$count" == "2" ]; then
    rpc_type=${config[1]}
  elif [ "$count" == "3" ]; then
    dir=${config[1]}
  fi
  
  if (( count < 4 )); then
    continue
  else
    client=${config[3]}
    copy_data
  fi 
done
