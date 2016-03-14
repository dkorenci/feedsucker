#!/bin/bash

# params: $1 - location of the bin folder of java runtime environment, 
# leave empty to work with JRE tools on system path 

# has to be run from Feedsucker root folder (with lib and src subfolders)

JAVA_BIN=$1 #"/usr/lib/jvm/java-7-openjdk-amd64/bin/"
#LOG="build_log.txt" ; `rm $LOG` 
ARCHIVE=Feedsucker.jar

# CLEAN
if [ -f $ARCHIVE ]; then
  rm $ARCHIVE
fi
for class in $(find src -regex ".*\.class");
do
  if [ -f $class ]; then
    rm $class
  fi  
done


# COMPILE
JAVAC="$JAVA_BIN""javac"
# put all the .jar files in lib directory into cp variable
SEP=':' # classpath separator symbol, it is ';' on some systems
cp=""
for jar in $(find `pwd` -regex ".*\.jar"); # use pwd to get absolute paths
do
  cp="$jar$SEP$cp"  
done
# put all the .java files into srcfiles variable
cd src # operate from source folder
srcfiles=""
for s in $(find -regex ".*\.java");
do
  srcfiles="$s $srcfiles" 
done
# run compiler
$JAVAC -classpath $cp $srcfiles #>> $LOG 2>&1
cd .. # return to root folder


# CREATE JAR
# create manifest.txt
cp=""
for jar in $(find lib -regex ".*\.jar");
do
  # the '\space\n\space' seprator is necessary, because of manifest file format
  cp="$jar \n $cp"  
done
echo -e "Class-Path: $cp" > manifest.txt
echo "Main-Class: feedsucker.core.FeedsuckerApp" >> manifest.txt
JAR="$JAVA_BIN""jar" #jar command
#echo 'CREATE JAR: ' >> $LOG
$JAR cfm $ARCHIVE manifest.txt -C src . #>> $LOG 2>&1


# clean *.class files
for class in $(find src -regex ".*\.class");
do
  if [ -f $class ]; then
    rm $class
  fi  
done