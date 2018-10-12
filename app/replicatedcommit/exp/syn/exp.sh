#DB type
db=$1
shift

#server cpu set
server_cpu=$1
shift

#Output path 
output_path=$1
shift

#A folder name consisting of basic configurations
config=$1
shift

#The ith experiment under the configuration
i=$1
shift

#exp wait time
exp_wait_time=$1
shift


start="`date`"
echo "$config exp $i begins @ $start" 

output_dir="$config/$i"
./start-exp.sh $db $server_cpu
sleep $exp_wait_time
./copy-client-log.sh $output_path $output_dir
./stop-exp.sh -f -a

end="`date`"
echo "$config exp $i finishes @ $end"
echo "$config exp $i Start=$start End=$end"
