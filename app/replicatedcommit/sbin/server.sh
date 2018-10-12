sbin="`dirname "$0"`"
sbin="`cd "$sbin"; pwd`"

source $sbin/func.sh

OPTIONS="-Dlog4j.configuration=file:./log4j.properties"
APP=replicatedcommit-0.1-jar-with-dependencies.jar
CLS="rc.benchmark.Server"

USAGE="
Usage: [-u user] [-k ssh-key-id] [-a host-ip-addr] [-e exec-dir] \
  <-d DC-id> <-r shard-id> <-i ip> <-p port> <-c config> <-t|s|g> [-l log file] [-b]
  [-z #cpu]
\t-h help

\t-u username, default is $USER
\t-k ssh key identity file
\t-a host machine ip address (can be different from rc server ip addr)
\t-e execution directory

\t-d datacenter id
\t-r shard id
\t-i rc server ip address
\t-p port
\t-c configuration file
\t-t tradrpc
\t-s specrpc
\t-g grpc
\t-l log file for both stdout and stderr
\t-b executes the program in background
\t-z pins the # of cpus for a server process.
"
usage()
{
  echo -e "$USAGE"
}

# Reset in case getopts has been used previously in the shell.
OPTIND=1         
while getopts "u:k:a:e:d:r:i:p:c:l:z:tsghb" opt; do
  case $opt in
    u)
      #Overrises $USER
      USER="$OPTARG"
      ;;
    k)
      SSH_ID_FILE="$OPTARG"
      ;;
    a)
      HOST_IP="$OPTARG"
      ;;
    e)
      EXEC_DIR="$OPTARG"
      ;;
    d)
      DC_ID="$OPTARG"
      ;;
    r)
      SHARD_ID="$OPTARG"
      ;;
    i)
      IP="$OPTARG"
      ;;
    p)
      PORT="$OPTARG"
      ;;
    c)
      CONFIG="$OPTARG"
      ;;
    t)
      TRAD_RPC="-t"
      ;;
    s)
      SPEC_RPC="-s"
      ;;
    g)
      G_RPC="-g"
      ;;
    l)
      LOG_FILE="$OPTARG"
      ;;
    b)
      BACKGROUND="&"
      ;;
    z)
      CPU_NUM="$OPTARG"
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

RPC="${TRAD_RPC}${SPEC_RPC}${G_RPC}"

if [ -z $DC_ID ] || \
  [ -z $SHARD_ID ] || \
  [ -z $IP ] || \
  [ -z $PORT ] || \
  [ -z $CONFIG ] || \
  [ -z $RPC ]; then
  usage;
  exit 1
fi

ARGS="-dc $DC_ID -shard $SHARD_ID -ip $IP -port $PORT \
  -config $CONFIG $RPC"
  #-config $CONFIG $TRAD_RPC $SPEC_RPC $G_RPC"
server_id="$DC_ID-$SHARD_ID"

if [ -n "$LOG_FILE" ]; then
  LOG="&>$LOG_FILE"
fi

if [ ! -z $SSH_ID_FILE ]; then
  if [ ! -f $SSH_ID_FILE ]; then
    echo "ssh identity file: $SSH_ID_FILE does not exist."
    usage;
    exit 1
  fi
  SSH_OPTION="-i $SSH_ID_FILE"
fi

if [ -z $HOST_IP ]; then
  HOST_IP="$IP"
fi

if [ -n $EXEC_DIR ]; then
  cmd="mkdir -p $EXEC_DIR; cd $EXEC_DIR"
fi

process_cmd="java"
if [ -n "$CPU_NUM" ]; then
  process_cmd="taskset 0x$CPU_NUM java"
fi

if [ -n "$BACKGROUND" ]; then
  #BACKGROUND="& echo \$! > ${server_id}.pid"
  cmd="$cmd; \
    ${process_cmd} $OPTIONS -cp $APP $CLS $ARGS $LOG $BACKGROUND \
    echo \$! > server-${server_id}.pid.log"
else
  cmd="$cmd; ${process_cmd} $OPTIONS -cp $APP $CLS $ARGS $LOG"
fi



#cmd="$cmd; java $OPTIONS -cp $APP $CLS $ARGS $LOG $BACKGROUND echo \$! > ${server_id}.pid"
#cmd="$cmd; java $OPTIONS -cp $APP $CLS $ARGS $LOG $BACKGROUND echo $!"
#cmd="ssh $SSH_OPTION $USER@$HOST_IP \"$cmd\""
ssh $SSH_OPTION $USER@$HOST_IP "$cmd"

#run_cmd $cmd
#eval "$cmd"
#pid=$!
#echo $pid >> pid-servers.log
