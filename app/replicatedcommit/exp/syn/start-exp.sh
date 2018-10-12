db=$1
shift
cpu_num=$1

source ./deploy.conf
server_list="$DEPLOY_RC_DIR/$DEPLOY_SERVER_LIST_FILE"
echo "server list: $server_list"
client_list="$DEPLOY_RC_DIR/$DEPLOY_CLIENT_LIST_FILE"
echo "client list: $client_list"

../../sbin/start-servers.sh $server_list $cpu_num

echo "Waits for servers to start up"
sleep 15
if [ "$db" == "BDB" ] || [ "$db" == "bdb" ]; then
  sleep 150
fi

../../sbin/start-clients.sh $client_list
