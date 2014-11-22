DB=$1
USER=$2
./delete_database.sh $DB
./create_database.sh $DB $USER

