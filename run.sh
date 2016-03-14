# params: $1 - location of the bin folder of java runtime environment, 
# leave empty to work with JRE tools on system path 

JAVA_BIN=$1 #"/usr/lib/jvm/java-7-openjdk-amd64/bin/"
JAVA="$JAVA_BIN""java" # java 7 or above required
# set max. heap size to 4 gigs, use runtime garbage collection
ARGS="-Xmx4g -XX:+UseConcMarkSweepGC" #-Dfile.encoding=UTF-8 
# run Feedsucker and write PID 
$JAVA -jar $ARGS Feedsucker.jar > run_output.txt 2>&1 &
echo $! > pid.txt

