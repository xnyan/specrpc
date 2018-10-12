#RPC framework type: t for TradRPC, s for SpecRPC, g for gRPC
rpc_type="t"

source exp-common.sh

echo "DEPLOY servers"
#Servers
$sbin/deploy.sh -d $deploy_config_file -l $server_list -s

echo "DEPLOY clients"
#Clients
$sbin/deploy.sh -d $deploy_config_file -l $client_list -c
