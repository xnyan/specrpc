#!/bin/bash
sbin="`dirname "$0"`"
sbin="`cd "$sbin"; pwd`"
source $sbin/func.sh

USAGE="
Usage: <-l client-list> [-f] [-r] [-a] [-d file] [-p]
\t-h help

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
while getopts "l:d:fraph" opt; do
  case $opt in
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

if [ -z $client_list_file ]; then
  usage; exit 1
fi

read_config $client_list_file
count=0
for config in "${CONFIG_LIST[@]}";
do
  count=$((count+1))
  #Parses each line
  config=($config)
  if (( count < 3 )); then
    continue
  elif [ "$count" == "3" ]; then
    #Client process execution directory
    eval EXEC_DIR=${config[1]}
    echo "Execution directory: $EXEC_DIR"
  else
    echo "Stoping clients with config: ${config[@]}"
    ip=${config[3]}
    cmd="cd $EXEC_DIR"
    if [ "$killall_mode" == "killall java" ]; then
      cmd="cd $EXEC_DIR; killall java"
    fi
    
    dcId="${config[0]}"
    machineId="${config[1]}"
    cNum="${config[2]}"
    for((n=1; n <=$cNum; n++));
    do
      cId="$dcId-$machineId-$n"
      if [ "$killall_mode" != "killall java" ]; then
        cmd="$cmd; pid=\`cat client-${cId}.pid.log\`; kill \$pid" 
      fi
      if [ "$clean" == "clean" ]; then
        cmd="$cmd; rm client-${cId}.log client-${cId}.pid.log"
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
  fi
done

wait
