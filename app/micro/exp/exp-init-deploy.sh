#RPC framework type: t for TradRPC, s for SpecRPC, g for gRPC
rpc_type="t"

source exp-common.sh
#sbin="../sbin"
#deploy_config_file="./deploy.conf"
#
#source $deploy_config_file
#server_list="$DEPLOY_SRC_DIR/$DEPLOY_SERVER_LIST_FILE"
#echo "server list: $server_list"
#client_list="$DEPLOY_SRC_DIR/$DEPLOY_CLIENT_LIST_FILE"
#echo "client list: $client_list"

##Deployment
#Makes sure that there is no old rpc signature file in the deploy config folder
rm $DEPLOY_SRC_DIR/$DEPLOY_RPC_SIG_FILE

echo "DEPLOY servers"
##Deploy server and client files
#
#Servers
#$sbin/deploy.sh -d $deploy_config_file -l $server_list -s -b -p
$sbin/deploy.sh -d $deploy_config_file -l $server_list -s -b
#$sbin/deploy.sh -d $deploy_config_file -l $server_list -s

echo "DEPLOY clients"
#
#Clients
#$sbin/deploy.sh -d $deploy_config_file -l $client_list -c  -p
$sbin/deploy.sh -d $deploy_config_file -l $client_list -c

#Generate RPC signatures
#Removes any existing rpc signature file on the server 
#TODO handle differet rpc signature name
rpc_sig_file=$DEPLOY_RPC_SIG_FILE
$sbin/stop-servers.sh -c $deploy_config_file -l $server_list -f -a -d $rpc_sig_file

$sbin/start-servers.sh $deploy_config_file $server_list $rpc_type
echo "Waits for servers to start up and generate rpc signatures"
sleep 2

#Collects rpc signatures
$sbin/server-rpc-sig.sh $deploy_config_file $server_list
$sbin/stop-servers.sh -c $deploy_config_file -l $server_list -f -a

#Deploys rpc signatures to servers and clients
$sbin/deploy-rpc-sig.sh -s -d $deploy_config_file -l $server_list
$sbin/deploy-rpc-sig.sh -c -d $deploy_config_file -l $client_list
