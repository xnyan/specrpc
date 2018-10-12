#!/usr/bin/env bash
sbin="`dirname "$0"`"
sbin="`cd "$sbin"; pwd`"
source $sbin/func.sh

USAGE="
Usage: <-c client-list file> <-s server-list file> <-d dir stored result>

\t -c client-list file
\t -s server-list file
\t -d dir stord result
"
usage()
{
  echo -e "$USAGE"
}


while getopts "c:s:d:" opt; do
  case $opt in
    c)
      client_list_file="$OPTARG"
      ;;
    s)
      server_list_file="$OPTARG"
      ;;
    d)
      dir="$OPTARG"
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


if [ -z $client_list_file ] || [ -z $server_list_file ]; then
  usage; exit 1
fi

#client_machine=$(awk -F ' ' '!/^ *#/ {print $2}' $client_list_file | awk '!NF || !seen[$0]++' )
#server_machine=$(awk -F ' ' '!/^ *#/ {print $2}' $server_list_file | awk '!NF || !seen[$0]++' )

read_config $client_list_file
for c in "${CONFIG_LIST[@]}"; do
  c=($c)
  mId=${c[0]}
  ip=${c[2]}
  ssh $ip "vmstat 1 > $dir/client-$mId-vmstat.log &"
  echo "Start vmstart at client" $mId $ip
done

read_config $server_list_file
for s in "${CONFIG_LIST[@]}"; do
  s=($s)
  sId=${s[0]}
  ip=${s[1]}
  ssh $ip "vmstat 1 > $dir/server-$sId-vmstat.log &" 
  echo "Start vmstart at server" $sId $ip
done

