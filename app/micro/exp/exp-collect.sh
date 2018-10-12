dst_dir=$1
if [ -z $dst_dir ]; then
  echo "Usage: <dst dir>"; exit 1
fi

source exp-common.sh
source $sbin/func.sh

mkdir -p $dst_dir
src_dir=$DEPLOY_DST_DIR

#Clients
read_config $client_list
for config in "${CONFIG_LIST[@]}"
do
  echo "$config"
  config=($config)
  ip=${config[2]}
  scp $ip:$src_dir/client-*.log $dst_dir/ 
  rm $dst_dir/client-*.pid.log
done

#Servers
read_config $server_list
for config in "${CONFIG_LIST[@]}"
do
  echo "$config"
  config=($config)
  ip=${config[1]}
  scp $ip:$src_dir/server-*.log $dst_dir/ 
  rm $dst_dir/server-*.pid.log
done
