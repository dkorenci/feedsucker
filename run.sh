JAVA_BIN=$1 #"/usr/lib/jvm/java-7-openjdk-amd64/bin/"
JAVA="$JAVA_BIN""java" # java 7 or above required
# set max. heap size to 4 gigs, use runtime garbage collection
ARGS="-Xmx4g -XX:+UseConcMarkSweepGC" #-Dfile.encoding=UTF-8 
$JAVA -jar $ARGS RssSucker.jar > run_output.txt &
echo $! > pid.txt

