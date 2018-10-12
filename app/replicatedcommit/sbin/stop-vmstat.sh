#!/usr/bin/env bash

USAGE="
Usage: <-c client-list file> <-s server-list file>

\t -c client-list file
\t -s server-list file
"
usage()
{
  echo -e "$USAGE"
}


while getopts "c:s:" opt; do
  case $opt in
    c)
      client_list_file="$OPTARG"
      ;;
    s)
      server_list_file="$OPTARG"
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

client_machine=$(awk -F ' ' '!/^ *#/ {print $2 }' $client_list_file | awk '!NF || !seen[$0]++' )

server_machine=$(awk -F ' ' '!/^ *#/ {print $2}' $server_list_file | awk '!NF || !seen[$0]++' )

for c in ${client_machine[@]}; do
    ssh $c "pkill -f 'vmstat 1'"
    echo "Stop vmstart at client" $c
done

for s in ${server_machine[@]}; do
    ssh $s "pkill -f 'vmstat 1'"
    echo "Stop vmstart at server" $s
done

