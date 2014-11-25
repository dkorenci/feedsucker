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

ACTION=$1

if [ "$ACTION" == "START" ]; then
  JAVA="java" # java 7 or above required
  LIB="\"lib/*;lib/hibernate-4.3.5/jpa/*;lib/hibernate-4.3.5/required/*\""
  MAIN_CLASS="rsssucker.core.RssSuckerApp"
  CMD="$JAVA -cp $LIB $MAIN_CLASS"
  echo $CMD
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
