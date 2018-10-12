
benchmark_config_file=$1
if [ -z $benchmark_config_file ]; then
  echo "Usage: <benchmark config file>"; exit 1;
fi

RESULT_DIR="./exp-result"
mkdir -p $RESULT_DIR

source exp-common.sh
source ../sbin/func.sh

read_config $benchmark_config_file

#Init deployment that builds the binary and generates RPC signatures
./exp-init-deploy.sh

for config in "${CONFIG_LIST[@]}"
do
  echo "Experiments with configurations: $config"
  
  #Generates micro config file
  ./gen_micro_config.sh "$config"

  #Deploy the new config file
  ./exp-quick-deploy.sh
  
  config=(`echo $config | sed "s/(//g; s/)//g; s/,//g"`)
  res_dir="micro"
  for par in ${config[@]}
  do
    res_dir="$res_dir-$par"
  done
  res_dir="$RESULT_DIR/$res_dir"

  for i in {1..5}
  do
    for rpc_type in "t" "s" "g"
    do
      echo "Experiments $rpc_type $i with config='${config[@]}'"
      dst_dir="$res_dir/$rpc_type-$i"
      ./exp-run.sh $rpc_type $dst_dir
      sleep 1
      echo "Experiments done $rpc_type $i with config='${config[@]}'"
    done
  done
done
