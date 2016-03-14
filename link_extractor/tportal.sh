#!/usr/bin/bash

day="11"
url="http://localhost/tportal$day""12.html" #"file://var/www/tportal$day12.html"
file="/datafast/feedsucker_data/feedfill/tportal_$day.txt"
feed="http://www.tportal.hr/vijesti/hrvatska/"
date="$day.12.2015-12:00:00"

echo ./extract.py "$url" "$file" "$feed" "$date"
./extract.py "$url" "$file" "$feed" "$date"
