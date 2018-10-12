#!/bin/bash
sbin="`dirname "$0"`"
sbin="`cd "$sbin"; pwd`"
source $sbin/func.sh

USAGE=" 
Usage: [-u user] [-k ssh-key-id] \
  <-s|c> <-d deploy.conf> <-l machine-list.conf>

\t-h help

\t-u username, default is $USER
\t-k ssh key identity file
\t-s deploy for servers
\t-c deploy for clients
\t-d a config file containing the files to be deployed
\t-l machine list
"

usage()
{
  echo -e "$USAGE"
}

OPTIND=1         
while getopts "u:k:d:l:sch" opt; do
  case $opt in
    u)
      #Overrises $USER
      USER="$OPTARG"
      ;;
    k)
      SSH_ID_FILE="$OPTARG"
      ;;
    s)
      IP_IDX=1
      ;;
    c)
      IP_IDX=2
      ;;
    d)
      deploy_config_file="$OPTARG"
      ;;
    l)
      machine_list_file="$OPTARG"
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

DEPLOY_FILES="$DEPLOY_SRC_DIR/$DEPLOY_RPC_SIG_FILE"

#Execution directory
eval EXEC_DIR=${DEPLOY_DST_DIR}
echo "Execution directory: $EXEC_DIR"

read_config $machine_list_file

for config in "${CONFIG_LIST[@]}";
do
  #Parses each line
  config=($config)
  ip=${config[$IP_IDX]}
  echo "Copying $DEPLOY_FILES to machine $ip:$EXEC_DIR"
  ssh $SSH_OPTION $USER@$ip "mkdir -p $EXEC_DIR"
  scp $SSH_OPTION $DEPLOY_FILES $USER@$ip:$EXEC_DIR/
  echo "Copied $DEPLOY_FILES to machine $ip:$EXEC_DIR"
done


