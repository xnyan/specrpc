#!/usr/bin/env bash

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

client_machine=$(awk -F ' ' '!/^ *#/ {print $2}' $client_list_file | awk '!NF || !seen[$0]++' )

server_machine=$(awk -F ' ' '!/^ *#/ {print $2}' $server_list_file | awk '!NF || !seen[$0]++' )

for c in ${client_machine[@]}; do
    mkdir -p client-$c-log
    scp $c:$dir/network.log client-$c-log/
    echo "Get network log from client" $c
done

for s in ${server_machine[@]}; do
    mkdir -p server-$s-log
    scp $s:$dir/network.log server-$s-log/
    echo "Get network log from server" $s
done

