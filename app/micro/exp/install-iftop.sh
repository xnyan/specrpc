usage="Usage: <machine list file>"
if [ -z $1 ]; then
  echo "$usage"; exit 2
fi
machine_list=$1
machine_list=`cat $machine_list`
machine_list=`echo "$machine_list"|sed "s/#.*$//;/^$/d"`
machine_list=($machine_list)


for m in ${machine_list[@]}; do
    ssh $m "sudo apt-get install -y libpcap-dev; sudo apt-get install -y ncurses-dev; cd ~/utils/iftop/iftop-1.0pre4; ./configure; make; sudo make install"
done
