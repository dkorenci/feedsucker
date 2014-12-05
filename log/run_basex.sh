JAVA=java #"/usr/lib/jvm/java-7-openjdk-amd64/bin/java"
FILE=$1
awk -f close_log.awk $FILE > log_file.xml
$JAVA -jar BaseX79.jar log_file.xml
rm log_file.xml
