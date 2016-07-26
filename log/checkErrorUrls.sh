# check if the urls in the errorUrl.txt file are saved in the database
# arg1 database arg2 number of texts
# print texts and titles ordered descending by datesaved
DATABASE=$1
USER=feedsucker
PASSW=feedsucker
ERRORURLS="errorUrlAll.txt"
TMPOUT=tmpout.txt
PGPASSFILE="`pwd`/.pgpass"
# create file with postgres passwords to enable
#  running psql without it asking for password
# change connection data if necessary
echo "localhost:5432:$DATABASE:$USER:$PASSW" >> $PGPASSFILE
chmod 600 $PGPASSFILE
export PGPASSFILE
#DB_COMMAND="COPY \
#(SELECT datesaved, substring(text from 1 for 100) FROM feedarticle ORDER BY datesaved DESC LIMIT $NUMT) \
#TO STDOUT WITH CSV" 
COUNTER=0
echo "Number of error URLs: `cat $ERRORURLS | wc -l`"
for url in `cat $ERRORURLS` ; do
	#echo $f
	DB_COMMAND="SELECT COUNT(*) FROM feedarticle WHERE url='$url'"
	#DB_INPUT="$PASSW\n$DB_COMMAND"
	#echo $DB_INPUT
	#echo $DB_COMMAND
	psql -U $USER -w -h localhost -d $DATABASE -c "$DB_COMMAND" > $TMPOUT
	res=`cat $TMPOUT | grep '^[ ]*1$' | wc -l`
	#echo $res
	if [ "$res" = "1" ]; then
		COUNTER=$((COUNTER + 1))
	fi		
done
echo "Number of error URLs saved $COUNTER"
rm $PGPASSFILE