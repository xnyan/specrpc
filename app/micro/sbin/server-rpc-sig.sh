#!/bin/bash
sbin="`dirname "$0"`"
sbin="`cd "$sbin"; pwd`"
source $sbin/func.sh

deploy_config=$1
shift
server_list_file=$1

if [ -z $deploy_config ] || [ -z $server_list_file ]; then
  echo "Usage: <deploy_config> <server-list>"; exit 1
fi

source $deploy_config

dst_dir=$DEPLOY_SRC_DIR
rpc_sig_file=$DEPLOY_RPC_SIG_FILE
tmp="rpc_sig.tmp"
rm $tmp
#Server process execution directory
eval EXEC_DIR=${DEPLOY_DST_DIR}
echo "Execution directory: $EXEC_DIR"

read_config $server_list_file

for config in "${CONFIG_LIST[@]}";
do
  #Parses each line
  config=($config)
  s_id="${config[0]}"
  ip="${config[1]}"
  echo "Copying rpc signature file from server ${s_id} @ $ip"
  scp $ip:$EXEC_DIR/$rpc_sig_file ./$rpc_sig_file
  cat $rpc_sig_file >> $tmp
  echo "Finised copying rpc signature file from server ${s_id} @ $ip"
done
rm $rpc_sig_file
mv $tmp $dst_dir/$rpc_sig_file
echo "All servers' rpc signatures have been stored to file $dst_dir/$rpc_sig_file"

