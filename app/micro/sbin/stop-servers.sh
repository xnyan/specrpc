#!/bin/bash
sbin="`dirname "$0"`"
sbin="`cd "$sbin"; pwd`"
source $sbin/func.sh

USAGE="
Usage: <-c deploy-config> <-l server-list> [-f] [-r] [-a] [-d file] [-p]
\t-h help

\t-c deploy config file
\t-l server list file
\t-f killall java processes
\t-r removes the specified server's log
\t-a removes any server log
\t-d removes the spefific file on the server
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
      server_list_file="$OPTARG"
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

if [ -z $deploy_config ] || [ -z $server_list_file ]; then
  usage; exit 1
fi

source $deploy_config
read_config $server_list_file

#Server process execution directory
eval EXEC_DIR=${DEPLOY_DST_DIR}
echo "Execution directory: $EXEC_DIR"

for config in "${CONFIG_LIST[@]}";
do
  #Parses each line
  config=($config)
  echo "Stoping server with config: ${config[@]}"
  sId="${config[0]}"
  ip=${config[1]}
  cmd="cd $EXEC_DIR; pid=\`cat server-${sId}.pid.log\`; kill \$pid" 
  if [ "$killall_mode" == "killall java" ]; then
    cmd="cd $EXEC_DIR; killall java"
  fi
  if [ "$clean" == "clean" ]; then
    cmd="$cmd; rm server-${sId}.log server-${sId}-net.log server-${sId}-vmstat.log server-${sId}.pid.log"
  elif [ "$clean" == "cleanall" ]; then
    cmd="$cmd; rm server-*.log"
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
  echo "Stoped server with config: ${config[@]}"
done

wait
