# get error urls form all the log folders, remove duplicates, and merge into one file
# must be run from log folder containing individual logs

ERROR_URL_FILE="errorUrl.log"
TMP_FILE="tmpurl"
OUT_FILE="errorUrlAll.txt"

if [ -f $TMP_FILE ]; then
  rm $TMP_FILE
fi

for d in */ ; do
    echo "$d$ERROR_URL_FILE"
    # remove duplicates and append to tmp
    sort -u "$d$ERROR_URL_FILE" >> $TMP_FILE
done

# remove duplicates and blank lines from tmp
sort -u $TMP_FILE | grep -v '^\s*$' > $OUT_FILE