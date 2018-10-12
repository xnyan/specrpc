#Uses the idx (1..n) config in the benchmark config file. Default is the first.
usage() {
  echo "Usage: [the i th (1~n) config in the benchmark config file]"
}

conf_idx=$1
if [ -z $conf_idx ]; then
  usage
  conf_idx=1
fi

deploy_conf="./deploy.conf"
benchmark_conf="./benchmark.conf"

if [ ! -f $deploy_conf ] || [ ! -f $benchmark_conf ]; then
 echo "$deploy_conf or $benchmark_conf does not exist."; exit 1  
fi


source $deploy_conf
source ../../sbin/func.sh

read_config $benchmark_conf
max_len=${#CONFIG_LIST[@]}
if (($conf_idx > $max_len)); then
  echo "Out of range error: using the ${conf_idx}th config, but only $max_len configs in $benchmark_conf."
  exit 1
fi

echo "Using the ${conf_idx}th config in $benchmark_conf"
((conf_idx-=1))
config="${CONFIG_LIST[${conf_idx}]}"
echo "Configuration: $config"
config=(`echo $config | sed "s/(//g; s/)//g; s/,//g"`)

#Benchmark configuration
BENCHMARK_STORAGE_TYPE_IDX=0
BENCHMARK_IS_SPEC_STATISTICS_IDX=1
BENCHMARK_TXN_RATE_IDX=2
BENCHMARK_EXEC_TIME_IDX=3
#Workload configuration
base=4
WORKLOAD_TYPE_IDX=$(($base))
WORKLOAD_RANDOM_SEED_IDX=$(($base+1))
WORKLOAD_ZIP_ALPHA_IDX=$(($base+2))
WORKLOAD_RETWIS_ADD_USER_IDX=$(($base+3))
WORKLOAD_RETWIS_FOLLOW_IDX=$(($base+4))
WORKLOAD_RETWIS_POST_IDX=$(($base+5))
WORKLOAD_RETWIS_LOAD_IDX=$(($base+6))
WORKLOAD_YCSBT_OP_NUM_IDX=$(($base+3))
WORKLOAD_YCSBT_READ_IDX=$(($base+4))
WORKLOAD_YCSBT_WRITE_IDX=$(($base+5))
WORKLOAD_YCSBT_RMW_IDX=$(($base+6))

STORAGE_CLS="rc.server.db.storage.InMemKeyValStore"
if [ "${config[$BENCHMARK_STORAGE_TYPE_IDX]}" == "bdb" ]; then
  STORAGE_CLS="rc.server.db.storage.BerkeleyDbKeyValStore"
fi

dc_list="1"
for i in `seq 2 1 $server_dc_num`; do
  dc_list="$dc_list,$i"
done
partition_list="1"
for i in `seq 2 1 $partition_num`; do
  partition_list="$partition_list,$i"
done

rc_config_file="$DEPLOY_RC_DIR/$DEPLOY_RC_CONFIG_FILE"
echo "
##RPC Framework
rc.rpc.framework=$rpc
rc.rpc.config.file=./$DEPLOY_RPC_CONFIG_FILE
rc.rpc.specrpc.readProxy.port=3000
rc.rpc.specrpc.statistics.enabled=${config[$BENCHMARK_IS_SPEC_STATISTICS_IDX]}

##Data Keys
rc.data.keyfile=./$DEPLOY_RC_KEY_FILE

##Client Configurations
rc.client.lib.id=1
rc.client.lib.threadPool.size=0
rc.client.lib.specrpc.readproxy.starttime=500

#Benchmark Configurations
rc.benchmark.running.time=${config[$BENCHMARK_EXEC_TIME_IDX]}
rc.benchmark.throughput=${config[$BENCHMARK_TXN_RATE_IDX]}

#Workload Configurations
rc.workload.random.seed=${config[$WORKLOAD_RANDOM_SEED_IDX]}
rc.workload.keydistribution=zipf
rc.workload.zipf.alpha=${config[$WORKLOAD_ZIP_ALPHA_IDX]}
rc.workload=${config[$WORKLOAD_TYPE_IDX]}
#YCSB+T
rc.workload.ycsbt.operation.num=${config[$WORKLOAD_YCSBT_OP_NUM_IDX]}
rc.workload.ycsbt.writeportion=${config[$WORKLOAD_YCSBT_WRITE_IDX]}
rc.workload.ycsbt.readportion=${config[$WORKLOAD_YCSBT_READ_IDX]}
rc.workload.ycsbt.rmwportion=${config[$WORKLOAD_YCSBT_RMW_IDX]}
#Retwis
rc.workload.retwis.adduser=${config[$WORKLOAD_RETWIS_ADD_USER_IDX]}
rc.workload.retwis.follow=${config[$WORKLOAD_RETWIS_FOLLOW_IDX]}
rc.workload.retwis.post=${config[$WORKLOAD_RETWIS_POST_IDX]}
rc.workload.retwis.load=${config[$WORKLOAD_RETWIS_LOAD_IDX]}

##Server Configurations
#Will be overwriteen at runtime
rc.dc.id=1
rc.dc.shard.id=2
rc.server.id=1-2

rc.dc.num=$server_dc_num
rc.dc.id.list=$dc_list
rc.dc.shard.num=$partition_num
rc.dc.shard.id.list=$partition_list

rc.txn.coordinator.threadPool.size=0
#Database Configurations
rc.db.storage.class=$STORAGE_CLS
rc.db.storage.bdb.env.dir=./
rc.db.storage.bdb.readOnly=false
rc.db.storage.bdb.txn=false
rc.db.storage.bdb.lockTimeout=PT5m
rc.db.storage.bdb.cacheSize=1g 
rc.db.txn.log.class=rc.server.db.log.AsyncTxnLogger
rc.db.txn.log.dir=./
rc.db.txn.log.file.prefix=server-
rc.db.txn.log.file.suffix=-txn.log
rc.db.txn.log.file.append=false
" > ${rc_config_file}

echo "RC configurations have been output to file $rc_config_file"
