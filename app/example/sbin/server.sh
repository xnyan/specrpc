sbin="`dirname "$0"`"
sbin="`cd "$sbin"; pwd`"

if [ -z "$SPECRPC_HOME" ]; then
  source $sbin/../../../config.sh
fi 

APP="$sbin/../target/specrpc-example-0.1-jar-with-dependencies.jar"
OPTIONS="-Dlog4j.configuration=file:$sbin/../../../conf/log4j.properties"
CLS="example.server.Server"

java $OPTIONS -cp $APP $CLS
