#!/usr/bin/bash
#url="http://www.jutarnji.hr/arhiva/vijesti/hrvatska/2015/12/10/"
day="14"
url="http://www.vecernji.hr/pregled-dana/2015-12-$day"
file="/datafast/rsssucker_data/feedfill/vecernji_$day.txt"
feed="http://www.vecernji.hr/hrvatska/"
date="$day.12.2015-12:00:00"

echo ./extract.py "$url" "$file" "$feed" "$date"
./extract.py "$url" "$file" "$feed" "$date"

