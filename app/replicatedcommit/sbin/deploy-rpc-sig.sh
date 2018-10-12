#!/bin/bash
sbin="`dirname "$0"`"
sbin="`cd "$sbin"; pwd`"
source $sbin/func.sh

USAGE=" 
Usage: [-u user] [-k ssh-key-id] \
  <-s|c> <-d deploy.conf> <-l machine-list.conf> [-i]

\t-h help
\t-u username, default is $USER
\t-k ssh key identity file
\t-s deploy for servers
\t-c deploy for clients
\t-d a config file containing the files to be deployed
\t-l machine list
\t-i deploy the key file for initialization
"

usage()
{
  echo -e "$USAGE"
}

OPTIND=1         
while getopts "u:k:d:l:scih" opt; do
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
    i)
      IS_DEPLOY_KEY_FILE="-k"
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
echo "DEPLOY_RPC_SIG_FILE=$DEPLOY_RPC_SIG_FILE"

DEPLOY_FILES="$DEPLOY_RC_DIR/$DEPLOY_RPC_SIG_FILE"

if [ -n "$IS_DEPLOY_KEY_FILE" ]; then
  DEPLOY_FILES="$DEPLOY_FILES $DEPLOY_RC_DIR/$DEPLOY_RC_KEY_FILE"
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
    scp $SSH_OPTION $DEPLOY_FILES $USER@$ip:$EXEC_DIR/
    echo "Copied $DEPLOY_FILES to machine $ip:$EXEC_DIR"
  fi
done


