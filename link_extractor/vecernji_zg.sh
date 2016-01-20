#!/usr/bin/bash
day="14"
url="http://www.vecernji.hr/pregled-dana/2015-12-$day"
file="/datafast/rsssucker_data/feedfill/vecernji_zg$day.txt"
feed="http://www.vecernji.hr/zg-vijesti/"
date="$day.12.2015-12:00:00"

echo ./extract.py "$url" "$file" "$feed" "$date"
./extract.py "$url" "$file" "$feed" "$date"

#!/usr/bin/env bash
