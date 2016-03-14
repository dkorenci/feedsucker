#!/usr/bin/bash

day="13"
#url="http://www.glas-slavonije.hr/Arhiva#&&pagearh=0&rubarh=1&datumarh=$day.12.2015."
#url="http://localhost/gshr$day""12.html"
url="/var/www/gshr$day""12.html"
file="/datafast/feedsucker_data/feedfill/glasslav_hr$day.txt"
feed="http://www.glas-slavonije.hr/Rss/Novosti"

date="$day.12.2015-12:00:00"

echo  "$url" "$file"
echo "$feed" "$date"

echo $feed > $file
echo $date >> $file
#cat $url | grep "href"
cat $url | sed 's/<h1>/\n/g' | grep "href=" | grep "/28" | sed 's/<a href=\"//g' | sed 's/<\/a>//g' \
    | sed 's/\">.*//g' >> $file


#echo ./extract.py "$url" "$file" "$feed" "$date"
#./extract.py "$url" "$file" "$feed" "$date"