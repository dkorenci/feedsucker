JAVA_BIN="/usr/lib/jvm/java-7-openjdk-amd64/bin/"
JAVA="$JAVA_BIN""java" # java 7 or above required
ARGS="-Dfile.encoding=UTF-8"
$JAVA -jar $ARGS RssSucker.jar > run_output.txt &
echo $! > pid.txt

