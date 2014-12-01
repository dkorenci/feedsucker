# params: $1 - user name, $2 - user password
sudo -u postgres psql -c "CREATE ROLE $1 CREATEDB LOGIN PASSWORD '$2'" 