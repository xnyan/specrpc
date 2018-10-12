#!/bin/bash
sbin="`dirname "$0"`"
sbin="`cd "$sbin"; pwd`"
source $sbin/func.sh

USAGE="
Usage: <-c deploy-config> <-l client-list> [-f] [-r] [-a] [-d file] [-p]
\t-h help

\t-c deploy config file
\t-l client list file
\t-f killall java processes
\t-r removes the specified client's log
\t-a removes any client log
\t-d removes the spefific file on the client
\t-p parallel mode
"

usage()
{
  echo -e "$USAGE"
}

OPTIND=1         
while getopts "c:l:d:fraph" opt; do
  case $opt in
    c)
      deploy_config="$OPTARG"
      ;;
    l)
      client_list_file="$OPTARG"
      ;;
    f)
      killall_mode="killall java"
      ;;
    r)
      clean="clean"
      ;;
    a)
      clean="cleanall"
      ;;
    d)
      del_file="$OPTARG"
      ;;
    p)
      parallel="-p"
      ;;
    h)
      usage
      exit 1
      ;;
    \?)
      echo "Invalid option: -$OPTARG" >&2
      usage; exit 1
      ;;
    :)
      echo "Option -$OPTARG requires an argument." >&2
      usage; exit 1
      ;;
  esac
done

if [ -z $deploy_config ] || [ -z $client_list_file ]; then
  usage; exit 1
fi

source $deploy_config
read_config $client_list_file

#Client process execution directory
eval EXEC_DIR=${DEPLOY_DST_DIR}
echo "Execution directory: $EXEC_DIR"

for config in "${CONFIG_LIST[@]}";
do
  #Parses each line
  config=($config)
  echo "Stoping clients with config: ${config[@]}"
  ip=${config[2]}
  cmd="cd $EXEC_DIR"
  if [ "$killall_mode" == "killall java" ]; then
    cmd="cd $EXEC_DIR; killall java"
  fi
  
  machineId="${config[0]}"
  cNum="${config[1]}"
  for((n=1; n <=$cNum; n++));
  do
    cId="$machineId-$n"
    if [ "$killall_mode" != "killall java" ]; then
      cmd="$cmd; pid=\`cat client-${cId}.pid.log\`; kill \$pid" 
    fi
    if [ "$clean" == "clean" ]; then
      cmd="$cmd; rm client-${cId}.log client-$machineId-net.log client-$machineId-vmstat.log client-${cId}.pid.log"
    fi
  done

  if [ "$clean" == "cleanall" ]; then
    cmd="$cmd; rm client-*.log"
  fi
  if [ ! -z $del_file ]; then
    cmd="$cmd; rm $del_file"
  fi
  
  echo $cmd
  
  if [ "$parallel" == "-p" ]; then
    ssh $USER@$ip "$cmd" &
    sleep 0.01
  else
    ssh $USER@$ip "$cmd"
  fi 
  echo "Stoped clients with config: ${config[@]}"
done

wait
