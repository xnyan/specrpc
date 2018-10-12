#!/bin/bash
usage() {
  echo "Usage: <rpc>"; echo "rpc: tradrpc specrpc grpc"; exit 1
}

deploy_conf="./deploy.conf"

#Overwrites the rpc variable in client_server_conf
rpc_type=$1
if [ -z $rpc_type ]; then
  usage
elif [ "$rpc_type" != "tradrpc" ] && [ "$rpc_type" != "specrpc" ] && [ "$rpc_type" != "grpc" ]; then
  usage
fi

if [ ! -f $deploy_conf ]; then
 echo "$deploy_conf does not exist."; exit 1  
fi

source ../../sbin/func.sh
source $deploy_conf

mkdir -p $DEPLOY_RC_DIR
client_list="$DEPLOY_RC_DIR/$DEPLOY_CLIENT_LIST_FILE"
rm $client_list
server_list="$DEPLOY_RC_DIR/$DEPLOY_SERVER_LIST_FILE"
rm $server_list

rc_config_file="$DEPLOY_RC_CONFIG_FILE"
for out_file in $client_list $server_list;
do
  append_file "#rc config file (on remote machine)" $out_file
  append_file "rc.config $rc_config_file" $out_file
  append_file "#rpc type" $out_file
  append_file "rpc.type $rpc_type" $out_file
  append_file "#exec dir (on remote machine)" $out_file
  append_file "exec.dir $exec_dir" $out_file
done

check_machine_num() {
  actual_machine_num=${#CONFIG_LIST[@]}
  actual_machine_list=$1
  expect_machine_num=$2
  expect_machine_num_conf_file=$3
  if (($actual_machine_num < $expect_machine_num)); then
    echo "There are not enough machines. Actual $actual_machine_num in $actual_machine_list. \
      Expected $expect_machine_num in $expect_machine_num_conf_file"
    exit 1
  fi
}

#client list
read_config $client_machine_list
check_machine_num $client_machine_list $client_machine_num $client_server_conf

append_file "#dcId, clientMachineId, #clientPerMachine, ip, port(specrpc proxy)" $client_list
client_per_machine=$(($client_num / $client_machine_num))
client_remainer_num=$(($client_num % $client_machine_num))
count=0
for config in "${CONFIG_LIST[@]}";
do
  config=($config)
  client_machine_id=${config[0]}
  client_machine_ip=${config[1]}
  cm=$client_per_machine
  if (($client_remainer_num != 0)); then
    ((cm+=1))
    ((client_remainer_num-=1))
  fi
  dc_id=$(($count % $client_dc_num + 1))
  ((count+=1))
  append_file "$dc_id $client_machine_id $cm $client_machine_ip $client_port_base" $client_list
  if (($count == $client_machine_num)); then
    break;
  fi
done

#server list
read_config $server_machine_list
check_machine_num $server_machine_list $server_machine_num $client_server_conf
required_server_machine_num=$(($server_dc_num * $partition_num))
if (($required_server_machine_num != $server_machine_num)); then
  echo "Error: #dc * #partition != #server machines in $client_server_conf"; exit 1
fi
append_file "#dcId, partitionId, ip, port" $server_list
count=0
for config in "${CONFIG_LIST[@]}";
do
  config=($config)
  server_machine_id=${config[0]}
  server_machine_ip=${config[1]}
  dc_id=$(($count / $partition_num + 1))
  partition_id=$(($count % $partition_num + 1))
  ((count+=1))
  append_file "$dc_id $partition_id $server_machine_ip $server_port_base" $server_list
  ((server_port_base+=1))
  if (($count == $server_machine_num)); then
    break;
  fi
done

