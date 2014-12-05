# argument1 : database name, argument2 user name (owner of the database)
DATABASE=$1
USER=$2
DB_COMMAND="CREATE DATABASE $DATABASE OWNER $USER ENCODING 'UTF8'" #LC_COLLATE 'POSIX' LC_CTYPE 'POSIX'
echo $DB_COMMAND
sudo -u postgres psql -c "$DB_COMMAND"
#foo="Hello"
#foo="$foo World"
#echo $foo
