#!/bin/bash
sbin="`dirname "$0"`"
sbin="`cd "$sbin"; pwd`"
source $sbin/func.sh

USAGE=" 
Usage: [-u user] [-k ssh-key-id] \
  <-s|c> <-d deploy.conf> <-l machine-list.conf> [-b] [-i] [-p]

\t-h help

\t-u username, default is $USER
\t-k ssh key identity file
\t-s deploy for servers
\t-c deploy for clients
\t-d a config file containing the files to be deployed
\t-l machine list
\t-b building the replicatedcommit executable
\t-i deploy the key file for initialization
\t-p parallel mode
"

usage()
{
  echo -e "$USAGE"
}

OPTIND=1         
while getopts "u:k:d:l:scbiph" opt; do
  case $opt in
    u)
      #Overrises $USER
      USER="$OPTARG"
      ;;
    k)
      SSH_ID_FILE="$OPTARG"
      ;;
    s)
      IP_IDX=2
      ;;
    c)
      IP_IDX=3
      ;;
    d)
      deploy_config_file="$OPTARG"
      ;;
    l)
      machine_list_file="$OPTARG"
      ;;
    b)
      IS_BUILD="-b"
      ;;
    i)
      IS_DEPLOY_KEY_FILE="-k"
      ;;
    p)
      PARALLEL_MODE="&"
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

if [ ! -z $SSH_ID_FILE ]; then
  if [ ! -f $SSH_ID_FILE ]; then
    echo "ssh identity file: $SSH_ID_FILE does not exist."
    usage;
    exit 1
  fi
  SSH_OPTION="-i $SSH_ID_FILE"
fi

if [ -z $IP_IDX ] || [ -z $deploy_config_file ] || [ -z $machine_list_file ]; then
  usage; exit 1
fi

if [ ! -f $deploy_config_file ] || [ ! -f $machine_list_file ]; then
  echo "$deploy_config_file or $machine_list_file does not exist."
  usage; exit 1
fi

source $deploy_config_file
echo "DEPLOY_RC_DIR=$DEPLOY_RC_DIR"
echo "DEPLOY_RC_APP_FILE=$DEPLOY_RC_APP_FILE"
echo "DEPLOY_RC_CONFIG_FILE=$DEPLOY_RC_CONFIG_FILE"
echo "DEPLOY_RC_KEY_FILE=$DEPLOY_RC_KEY_FILE"
echo "DEPLOY_RPC_CONFIG_FILE=$DEPLOY_RPC_CONFIG_FILE"
echo "DEPLOY_RPC_SIG_FILE=$DEPLOY_RPC_SIG_FILE"
echo "DEPLOY_LOG_CONFIG_FILE=$DEPLOY_LOG_CONFIG_FILE"

DEPLOY_FILES="$DEPLOY_RC_DIR/$DEPLOY_RC_APP_FILE $DEPLOY_RC_DIR/$DEPLOY_RC_CONFIG_FILE\
  $DEPLOY_RC_DIR/$DEPLOY_RPC_CONFIG_FILE $DEPLOY_RC_DIR/$DEPLOY_RPC_SIG_FILE\
  $DEPLOY_RC_DIR/$DEPLOY_LOG_CONFIG_FILE"
if [ -n "$IS_DEPLOY_KEY_FILE" ]; then
  DEPLOY_FILES="$DEPLOY_FILES $DEPLOY_RC_DIR/$DEPLOY_RC_KEY_FILE"
fi

if [ -n "$IS_BUILD" ]; then
  echo "Builds the executable."
  if [ -z $SPECRPC_HOME ]; then
    echo "Can not find \$SPECRPC_HOME. Tring to configure automatically..."
    . "$sbin/../../../config.sh"
  fi
  cur_dir="`pwd`"
  eval "cd $SPECRPC_HOME; ./build.sh"
  cd $cur_dir
  #TODO Stops the script if building fails.
  cp $SPECRPC_HOME/app/replicatedcommit/target/replicatedcommit-0.1-jar-with-dependencies.jar \
    $DEPLOY_RC_DIR/$DEPLOY_RC_APP_FILE
  echo "Copied the executable to $DEPLOY_RC_DIR/$DEPLOY_RC_APP_FILE"
fi

read_config $machine_list_file
count=0
for config in "${CONFIG_LIST[@]}";
do
  count=$((count+1))
  #Parses each line
  config=($config)
  if (( count < 3 )); then
    continue
  elif [ "$count" == "3" ]; then
    #Server process execution directory
    eval EXEC_DIR=${config[1]}
    echo "Execution directory: $EXEC_DIR"
  else
    ip=${config[$IP_IDX]}
    echo "Copying $DEPLOY_FILES to machine $ip:$EXEC_DIR"
    ssh $SSH_OPTION $USER@$ip "mkdir -p $EXEC_DIR"

    if [ -n "$PARALLEL_MODE" ]; then
      sleep 0.015
      scp $SSH_OPTION $DEPLOY_FILES $USER@$ip:$EXEC_DIR/ &
    else
      scp $SSH_OPTION $DEPLOY_FILES $USER@$ip:$EXEC_DIR/
    fi
    echo "Copied $DEPLOY_FILES to machine $ip:$EXEC_DIR"
  fi
done

wait
