sbin="../sbin"
deploy_config_file="./deploy.conf"

source $deploy_config_file
server_list="$DEPLOY_SRC_DIR/$DEPLOY_SERVER_LIST_FILE"
echo "server list: $server_list"
client_list="$DEPLOY_SRC_DIR/$DEPLOY_CLIENT_LIST_FILE"
echo "client list: $client_list"

micro_config_file="$DEPLOY_SRC_DIR/$DEPLOY_MICRO_CONFIG_FILE"
