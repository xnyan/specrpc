sbin="`dirname "$0"`"
sbin="`cd "$sbin"; pwd`"

source $sbin/func.sh

OPTIONS="-Dlog4j.configuration=file:./log4j.properties"
APP=specrpc-micro-0.1-jar-with-dependencies.jar
CLS="micro.benchmark.Client"

USAGE="
Usage: [-u user] [-k ssh-key-id] <-a addr-host-ip> [-e exec-dir] \
  [-m machine-id] [-n #clients] <-c config> <-t|s|g> \
  [-b]
\t-h help

\t-u username, default is $USER
\t-k ssh key identity file
\t-a host machine ip address
\t-e execution directory

\t-m machine id
\t-n client number
\t-c configuration file
\t-t tradrpc
\t-s specrpc
\t-g grpc
\t-b executes the program in background
"
usage()
{
  echo -e "$USAGE"
}

# Reset in case getopts has been used previously in the shell.
OPTIND=1         
while getopts "u:k:a:e:m:n:c:tsgbh" opt; do
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
    m)
      MACHINE_ID="$OPTARG"
      ;;
    n)
      CLIENT_NUM="$OPTARG"
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
    b)
      BACKGROUND="&"
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

if [ -z $HOST_IP ] || \
  [ -z $CONFIG ] || \
  [ -z $RPC ]; then
  usage;
  exit 1
fi

ARGS="-config $CONFIG $RPC"

if [ ! -z $SSH_ID_FILE ]; then
  if [ ! -f $SSH_ID_FILE ]; then
    echo "ssh identity file: $SSH_ID_FILE does not exist."
    usage;
    exit 1
  fi
  SSH_OPTION="-i $SSH_ID_FILE"
fi

if [ -n $EXEC_DIR ]; then
  cmd="mkdir -p $EXEC_DIR; cd $EXEC_DIR"
fi

if [ -z $MACHINE_ID ]; then
  MACHINE_ID=1
fi

client_num=1
if [ -n $CLIENT_NUM ]; then
  client_num=$CLIENT_NUM 
fi

for((n=1; n <= $client_num; n++));
do
  cId="$MACHINE_ID-$n"
  args="-id $cId"
  args="$args $ARGS"
  LOG="&> client-$cId.log"
  if [ -n "$BACKGROUND" ]; then
    cmd="$cmd; \
      java $OPTIONS -cp $APP $CLS $args $LOG $BACKGROUND \
      echo \$! > client-$cId.pid.log; \
      sleep 0.05"
  else
    cmd="$cmd; \
      java $OPTIONS -cp $APP $CLS $args $LOG"
  fi

done

echo $cmd
ssh $SSH_OPTION $USER@$HOST_IP "$cmd"

