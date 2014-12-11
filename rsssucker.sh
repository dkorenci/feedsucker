# rsssucker startup and shutdown script
# has to be executed from rsssucker (root) folder

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

ACTION=$1

if [ "$ACTION" == "START" ]; then
  JAVA_BIN=$(get_java_bin $2)
  ./run.sh $JAVA_BIN
elif [ "$ACTION" == "TOOL" ]; then
  # java must be specified explicitly for tools, for now, to fetch tool argument
  # use default_java instead of ""
  JAVA_BIN=$(get_java_bin $2)     
  "$JAVA_BIN""java" -jar RssSucker.jar $3
elif [ "$ACTION" == "BUILD" ]; then
  JAVA_BIN=$2
  ./build.sh $JAVA_BIN > build_out.txt 2>&1
elif [ "$ACTION" == "STOP" ]; then
  MSG_FILE="messages.txt" # file to send messages to program
  # commands to write to the file
  SHUTDOWN_NOW="close" 
  SHUTDOWN_FINNISH="finish_and_close"  
  # determines how to shutodown
  MODE=$(default_ifundef $2 "NOW")
  if [ "$MODE" == "NOW" ]; then     
    echo $SHUTDOWN_NOW > $MSG_FILE
  fi
  if [ "$MODE" == "WAIT" ]; then     
    echo $SHUTDOWN_FINNISH > $MSG_FILE
  fi  
fi	
