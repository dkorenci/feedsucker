# rsssucker startup and shutdown script
MSG_FILE="messages.txt"
SHUTDOWN_NOW="close"
SHUTDOWN_FINNISH="finish_and_close"
ACTION=$1
#if [ "$ACTION" == "START" ]; then
#fi	
if [ "$ACTION" == "STOP" ]; then
  MODE=$2   
  if [ "$MODE" == "NOW" ] || [ "$MODE" == "" ]; then     
    eval "echo $SHUTDOWN_NOW > $MSG_FILE"
  fi
  if [ "$MODE" == "WAIT" ]; then     
    eval "echo $SHUTDOWN_FINNISH > $MSG_FILE"
  fi  
fi	
