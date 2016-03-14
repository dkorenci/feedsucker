#!/usr/bin/bash

day="10"
page="27"
url="http://www.vijesti.rtl.hr/novosti/hrvatska/?stranica=$page"
file="/datafast/feedsucker_data/feedfill/rtl$page.txt"
feed="http://www.vijesti.rtl.hr/novosti/hrvatska/"

date="$day.12.2015-12:00:00"

echo ./extract.py "$url" "$file" "$feed" "$date"
./extract.py "$url" "$file" "$feed" "$date"