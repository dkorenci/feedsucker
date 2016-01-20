#!/usr/bin/bash
#url="http://www.jutarnji.hr/arhiva/vijesti/hrvatska/2015/12/10/"
day=13
url="http://www.jutarnji.hr/arhiva/vijesti/hrvatska/2015/12/$day/"
file="/datafast/rsssucker_data/feedfill/jutarnji_$day.txt"
feed="http://www.jutarnji.hr/rss?type=section&id=10"
date="$day.12.2015-12:00:00"

url="http://www.jutarnji.hr/arhiva/2015/12/10/"
file="/datafast/rsssucker_data/feedfill/jutarnji_all10.txt"

echo ./extract.py "$url" "$file" "$feed" "$date"
./extract.py "$url" "$file" "$feed" "$date"
