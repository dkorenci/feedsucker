# arg1 database arg2 number of texts
# print texts and titles ordered descending by datesaved
DATABASE=$1
NUMT=$2
#DB_COMMAND="COPY \
#(SELECT datesaved, substring(text from 1 for 100) FROM feedarticle ORDER BY datesaved DESC LIMIT $NUMT) \
#TO STDOUT WITH CSV" 
DB_COMMAND="SELECT datesaved, substring(text from 1 for 100) FROM feedarticle ORDER BY datesaved DESC LIMIT $NUMT;"
echo $DB_COMMAND
echo $DB_COMMAND | psql --username feedsucker --dbname $1
#sudo -u postgres psql -c @$DB_COMMAND"
