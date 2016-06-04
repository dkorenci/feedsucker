# create deploy folder from git repository
# ie clone repo into deploy folder and run setup actions
# pgsql database is created and 'feedsucker' login role has to exist

LABEL=$1 # label of the deploy
REPO="/data/code/feedsucker/"
OUT_FOLDER="feedsucker_$LABEL"
git clone $REPO $OUT_FOLDER
cd $OUT_FOLDER
# save current head commit id
git rev-parse HEAD > git_head_commit_id.txt
# ungit the deploy folder 
#rm -r .git
#rm .gitignore
# create JPA persistence config from template
cd src/META-INF
cp persistence.template.xml persistence.xml
cd ..; cd ..; 
# create properties file from template
cd config
cp feedsucker.properties.template feedsucker.properties
# create database of the deploy
cd ..
cd db_scripts
./create_database.sh "feedsucker_$LABEL" feedsucker
 
