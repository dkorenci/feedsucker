# merge XML log files from all the log folders into one XML log file
# arguments: 
# $1 type of log - "error", "info", "debug" or "root" (all log messages)
# $2 output file

LOG_TYPE=$1
OUT_FILE=$2

# open main log tag
echo "<log>" > $OUT_FILE

# traverse subfolders sorted by date, ascending
IFS='
'
for d in `ls -d */ --sort=t -r` ; do
    echo $d
    LOG_FILE="$d$LOG_TYPE.log"
    echo $LOG_FILE
    # remove start and end log tags from XML
    sed 's/<log>//;s/<\/log>//' $LOG_FILE >> $OUT_FILE    
    # remove duplicates and append to tmp
    #sort -u "$d$ERROR_URL_FILE" >> $TMP_FILE
done

# close main log tag
echo "</log>" >> $OUT_FILE