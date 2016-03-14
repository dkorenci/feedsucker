# feedsucker startup and shutdown script
# has to be executed from feedsucker (root) folder

# accepts two arguments, returns first if it is not empty string, else returns second
# used for getting default values for possibly empty parameters
function default_ifundef {   
  if [ "$1" == "" ] ; then
    echo $2  
  else echo $1
  fi
}

# return java path: if default path is designated return "", else return path
function get_java_bin {   
  if [ "$1" == "default_java" ] || [ "$1" == "" ] ; then
    echo ""  
  else echo $1
  fi
}

# MESSAGING CONSTANTS
# message strings, must be in sync with Messages class in the application
SHUTDOWN_NOW="close" 
SHUTDOWN_FINNISH="finish_and_close"  
#message file names, must be in sync with code that inits MessageFileMonitors
APP_MSG_FILE="messages.txt"
LOOP_MSG_FILE="loop_messages.txt"

# START SCRIPT LOGIC
ACTION=$1

if [ "$ACTION" == "START" ]; then
  JAVA_BIN=$(get_java_bin $2)
  ./run.sh $JAVA_BIN
elif [ "$ACTION" == "TOOL" ]; then
  # java must be specified explicitly for tools, for now, to fetch tool argument
  # use default_java instead of ""
  JAVA_BIN=$(get_java_bin $2) 
  JAVA_CMD="$JAVA_BIN""java"
  ARGS="-Xmx6g -XX:+UseConcMarkSweepGC"
  $JAVA_CMD -jar $ARGS Feedsucker.jar $2 $3
elif [ "$ACTION" == "LOOP" ]; then  
  if [ "$2" == "STOP" ]; then     
    echo $SHUTDOWN_NOW > $LOOP_MSG_FILE
  elif [ "$2" == "START" ]; then
    JAVA_BIN=$(get_java_bin $3) 
    JAVA_CMD="$JAVA_BIN""java"
    $JAVA_CMD -jar Feedsucker.jar $3 LOOP > loop_output.txt &
  fi   
elif [ "$ACTION" == "BUILD" ]; then
  JAVA_BIN=$2
  ./build.sh $JAVA_BIN > build_out.txt 2>&1
elif [ "$ACTION" == "STOP" ]; then
   # file to send messages to program
  # determines how to shutodown
  MODE=$(default_ifundef $2 "NOW")
  if [ "$MODE" == "NOW" ]; then     
    echo $SHUTDOWN_NOW > $APP_MSG_FILE
  fi
  if [ "$MODE" == "WAIT" ]; then     
    echo $SHUTDOWN_FINNISH > $APP_MSG_FILE
  fi  
fi	
