#!/usr/bin/bash

day="13"
url="http://www.slobodnadalmacija.hr/Linkovi/Arhiv/tabid/219/Datum/$day.12.2015/Default.aspx"
file="/datafast/feedsucker_data/feedfill/slobodna_hr$day.txt"
feed="http://www.slobodnadalmacija.hr/RssHrvatska.aspx"

date="$day.12.2015-12:00:00"

echo ./extract.py "$url" "$file" "$feed" "$date"
./extract.py "$url" "$file" "$feed" "$date"