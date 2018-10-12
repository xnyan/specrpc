Define deploy.conf benchmark.conf

Define rc-keys.conf log4j.properties, and rpc.conf 
Move the above three files to the specific folder that is defined in deploy.conf

Acording to deploy.conf, define the files that contain server and client machine list 

Generate the lists of client and server instnaces (running environment)

./gen-client-server-list.sh

Generate the rc-config file

./gen-rc-config.sh

To deploy servers and clients and to collect servers' rpc signature file

./init-deploy.sh

To run different workload with different benchmark settings, modify the benchmark.conf file, and specify the
ith benchmark config in the benchmark.conf file by running the following command:

./gen-rc-config.sh i

To run an experiment

./quick-deploy.sh
./start-exp.sh
./copy-client-log.sh
./stop-exp.sh
