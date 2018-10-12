
read_config() {
  CONFIG_FILE=$1
  CONFIG_LIST=()
  while read line;
  do
    config=`echo $line | sed "s/#.*$//;/^$/d"`
    if [ -z "$config" ]; then
      continue
    fi
    CONFIG_LIST=("${CONFIG_LIST[@]}" "$config")
  done<$CONFIG_FILE
}

run_cmd() {
  cmd="$@"
  eval $cmd
}

#Runs the cmd in background
run_cmd_bk(){
  cmd="$@"
  eval $cmd &
}

append_file() {
  echo $1 >> $2
}
