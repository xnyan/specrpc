#!/bin/bash

sbin="`dirname "$0"`"
sbin="`cd "$sbin"; pwd`"
source $sbin/func.sh

server_list_file=$1
shift
#rpc_sig_file="rpc-signatures"
rpc_sig_file=$1

if [ -z $server_list_file ] || [ -z $rpc_sig_file ]; then
  echo "Usage: <server-list> <rpc sig file>"
  exit 1
fi

tmp="rpc_sig.tmp"
rm $tmp

read_config $server_list_file
count=0
for config in "${CONFIG_LIST[@]}";
do
  count=$((count+1))
  #Parses each line
  config=($config)
  if [ "$count" == "1" ] || [ "$count" == "2" ]; then
    continue
  elif [ "$count" == "3" ]; then
    #Server process execution directory
    eval EXEC_DIR=${config[1]}
    echo "Execution directory: $EXEC_DIR"
  else
    s_id="${config[0]}-${config[1]}"
    ip="${config[2]}"
    echo "Copying rpc signature file from server ${s_id} @ $ip"
    scp $ip:$EXEC_DIR/$rpc_sig_file ./$rpc_sig_file
    cat $rpc_sig_file >> $tmp
    echo "Finised copying rpc signature file from server ${s_id} @ $ip"
  fi
done
mv $tmp $rpc_sig_file
echo "All servers' rpc signatures have been stored to file ./$rpc_sig_file"
