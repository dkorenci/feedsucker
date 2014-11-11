# argument1 : database name, argument2 user name (owner of the database)
# create a database using create_database.sql script
# database name and user name are taken from command line and substituted in the script
# user executing script must be able to sudo as postgres (default admin account that must exist)
DATABASE=$1
USER=$2
DB_COMMAND=`cat create_database.sql`
DB_COMMAND=${DB_COMMAND/'$USER'/$USER}
DB_COMMAND=${DB_COMMAND/'$DATABASE'/$DATABASE}
echo $DB_COMMAND
sudo -u postgres psql -c "$DB_COMMAND"
#foo="Hello"
#foo="$foo World"
#echo $foo
