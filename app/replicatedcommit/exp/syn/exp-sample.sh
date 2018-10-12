#Define rc-config.conf, client-list.conf and server-list.conf


#The number of cpus for a server proces, f is 1111, which means the first 4 CPUs
server_cpu="f"

output_path="./exp-data/"

#Time to wait for the exp to end, in seconds. This should be longer for the
#actual exp time since the script needs to set up the environment.
exp_wait_time=80

#Backend Storage Type: "InMemKv" for in-memory storage, "BDB" for using BerkleyDB
db="InMemKv"
zipf=0.75

./quick-deploy.sh

for rpc in "specrpc" "tradrpc" "grpc"
do
  # Output folder, e.g., consisting of basic configurations
  output_dir="zipf${zipf}"
  
  # Nnumber of measurements
  for i in {1..5}
  do
    exp.sh $db $server_cpu $output_path $output_dir $i $exp_wait_time
  done
#rpc
done
