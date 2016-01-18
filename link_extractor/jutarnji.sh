#!/usr/bin/bash
#url="http://www.jutarnji.hr/arhiva/vijesti/hrvatska/2015/12/10/"
url="http://www.jutarnji.hr/arhiva/vijesti/hrvatska/2015/12/11/"
file="/datafast/rsssucker_data/feedfill/jutarnji2.txt"
feed="http://www.jutarnji.hr/rss?type=section&id=10"
date="11.12.2015-12:00:00"

echo ./extract.py "$url" "$file" "$feed" "$date"
./extract.py "$url" "$file" "$feed" "$date"
