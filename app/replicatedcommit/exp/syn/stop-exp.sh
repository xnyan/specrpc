source ./deploy.conf
server_list="$DEPLOY_RC_DIR/$DEPLOY_SERVER_LIST_FILE"
echo "server list: $server_list"
client_list="$DEPLOY_RC_DIR/$DEPLOY_CLIENT_LIST_FILE"
echo "client list: $client_list"

../../sbin/stop-clients.sh -l $client_list $@
../../sbin/stop-servers.sh -l $server_list $@
