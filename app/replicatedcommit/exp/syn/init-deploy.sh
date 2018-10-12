source ./deploy.conf
server_list="$DEPLOY_RC_DIR/$DEPLOY_SERVER_LIST_FILE"
echo "server list: $server_list"
client_list="$DEPLOY_RC_DIR/$DEPLOY_CLIENT_LIST_FILE"
echo "client list: $client_list"

##Deployment
#Makes sure that there is no old rpc signature file in the deploy config folder
rm $DEPLOY_RC_DIR/$DEPLOY_RPC_SIG_FILE

##Deploy server and client files
#servers
#../../sbin/deploy.sh -d deploy.conf -l $server_list -s -b -i -p
../../sbin/deploy.sh -d deploy.conf -l $server_list -s -b -i
#clients
#../../sbin/deploy.sh -d deploy.conf -l $client_list -c -i -p
../../sbin/deploy.sh -d deploy.conf -l $client_list -c -i

#Generate RPC signatures
#Removes any existing rpc signature file on the server 
#TODO handle differet rpc signature name
rpc_sig_file=$DEPLOY_RPC_SIG_FILE
../../sbin/stop-servers.sh -l $server_list -f -a -d $rpc_sig_file

../../sbin/start-servers.sh $server_list
echo "Waits for servers to start up and generate rpc signatures"
sleep 15

#Collects rpc signatures
rpc_sig_file=$DEPLOY_RC_DIR/$DEPLOY_RPC_SIG_FILE
../../sbin/server-rpc-sig.sh $server_list $rpc_sig_file

../../sbin/stop-servers.sh -l $server_list -f -a
