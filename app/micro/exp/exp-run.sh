#RPC framework type: t for TradRPC, s for SpecRPC, g for gRPC
rpc_type=$1
if [ -z $rpc_type ]; then
  echo "Usage: <t|s|g> [result dir], t for TradRpc, s for SpecRpc, g for gRpc"; exit 1
fi
shift
res_dir=$1
if [ -z $res_dir ]; then
  echo "Using default result directory: ./"
  res_dir="./"
fi

source exp-common.sh
#sbin="../sbin"
#deploy_config_file="./deploy.conf"
#
#source $deploy_config_file
#server_list="$DEPLOY_SRC_DIR/$DEPLOY_SERVER_LIST_FILE"
#echo "server list: $server_list"
#client_list="$DEPLOY_SRC_DIR/$DEPLOY_CLIENT_LIST_FILE"
#echo "client list: $client_list"

#Servers
$sbin/start-servers.sh $deploy_config_file $server_list $rpc_type
sleep 1

#Starts Resource Monitor
$sbin/start-network.sh -c $client_list -s $server_list -d $DEPLOY_DST_DIR  
$sbin/start-vmstat.sh -c $client_list -s $server_list -d $DEPLOY_DST_DIR  

#Clients
$sbin/start-clients.sh $deploy_config_file $client_list $rpc_type

sleep 62

#Stops Resource Monitor
$sbin/stop-network.sh -c $client_list -s $server_list
$sbin/stop-vmstat.sh -c $client_list -s $server_list

#Collects results
./exp-collect.sh $res_dir

#Stops servers and clients
$sbin/stop-clients.sh -c $deploy_config_file -l $client_list -r
$sbin/stop-servers.sh -c $deploy_config_file -l $server_list -r

