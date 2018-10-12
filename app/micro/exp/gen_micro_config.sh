benchmark_config=$1
if [ -z "$benchmark_config" ]; then
  echo "Usage: <benchmark config list as one string, e.g. (one_hop, ...)>"; exit 1
fi

echo "Configuration: $benchmark_config"
config=(`echo $benchmark_config | sed "s/(//g; s/)//g; s/,//g"`)

source exp-common.sh

workload_type_idx=0
rpc_or_hop_num_idx=1
local_comp_idx=2
server_comp_before_rpc_idx=3
server_comp_after_rpc_idx=4
client_predict_idx=5
client_predict_correct_rate_idx=6
server_predict_idx=7
server_predict_correct_rate_idx=8

workload_type=${config[$workload_type_idx]}
if [ "$workload_type" == "one_hop" ]; then
  rpc_num=${config[$rpc_or_hop_num_idx]}
  rpc_hop_num=1
else
  rpc_num=1
  rpc_hop_num=${config[$rpc_or_hop_num_idx]}
fi 

echo "
rpc.framework=tradrpc

rpc.config.file=./rpc.conf
rpc.specrpc.statistics.enabled=true
random.seed=0
benchmark.running.time=PT60s
benchmark.throughput=10
workload.server.num=10
workload.req.data.length=64

workload.type=${config[$workload_type_idx]}
workload.req.rpc.num=$rpc_num
workload.req.rpc.hop.num=$rpc_hop_num
workload.client.local.comp.time.afterRpc=${config[$local_comp_idx]}
workload.server.comp.time.beforeRpc=${config[$server_comp_before_rpc_idx]}
workload.server.comp.time.afterRpc=${config[$server_comp_after_rpc_idx]}
spec.client.isPredict=${config[$client_predict_idx]}
spec.client.correct.rate=${config[$client_predict_correct_rate_idx]}
spec.server.isPredict=${config[$server_predict_idx]}
spec.server.correct.rate=${config[$server_predict_correct_rate_idx]}

spec.server.predict.point=before_rpc

server.id=1
server.ip=localhost
server.port=4001
client.id=1
" > $micro_config_file

echo "Micro config file is generated as $micro_config_file"
