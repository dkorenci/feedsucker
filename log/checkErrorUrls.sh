# check if the urls in the errorUrl.txt file are saved in the database
# arg1 database arg2 number of texts
# print texts and titles ordered descending by datesaved
DATABASE=$1
ERRORURLS="errorUrlAll.txt"
TMPOUT=tmpout.txt
#DB_COMMAND="COPY \
#(SELECT datesaved, substring(text from 1 for 100) FROM feedarticle ORDER BY datesaved DESC LIMIT $NUMT) \
#TO STDOUT WITH CSV" 
COUNTER=0
echo "Number of error URLs: `cat $ERRORURLS | wc -l`"
for url in `cat $ERRORURLS` ; do
	#echo $f
	DB_COMMAND="SELECT COUNT(*) FROM feedarticle WHERE url='$url'"
	#echo $DB_COMMAND
	echo $DB_COMMAND | psql --username rsssucker --dbname $1 > $TMPOUT
	res=`cat $TMPOUT | grep '^[ ]*1$' | wc -l`
	#echo $res
	if [ "$res" = "1" ]; then
		COUNTER=$((COUNTER + 1))
	fi	
done
echo "Number of error URLs saved $COUNTER"
