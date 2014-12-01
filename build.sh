# has to be run from RssSucker root folder (with lib and src subfolders)
JAVA_BIN="/usr/lib/jvm/java-7-openjdk-amd64/bin/"
#LOG="build_log.txt" ; `rm $LOG` 

# CLEAN
`rm RssSucker.jar`
for class in $(find src -regex ".*\.class");
do
  rm $class
done


# COMPILE
JAVAC="$JAVA_BIN""javac"
# put all the .jar files in lib directory into cp variable
SEP=':' # classpath separator symbol, it is ';' on some systems
cp=""
for jar in $(find `pwd` -regex ".*\.jar");
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
cd ..


# CREATE JAR
# create manifest.txt
cp=""
for jar in $(find lib -regex ".*\.jar");
do
  # the '\space\n\space' seprator is necessary, because of manifest file format
  cp="$jar \n $cp"  
done
#echo -e "Manifest-Version: 1.0\n" > manifest.txt
echo -e "Class-Path: $cp" > manifest.txt
echo "Main-Class: rsssucker.core.RssSuckerApp" >> manifest.txt
JAR="$JAVA_BIN""jar" #jar command
#echo 'CREATE JAR: ' >> $LOG
$JAR cfm RssSucker.jar manifest.txt -C src . #>> $LOG 2>&1
