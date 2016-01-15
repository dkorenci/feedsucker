url="http://www.jutarnji.hr/arhiva/vijesti/hrvatska/2015/12/10/"
file="/datafast/rsssucker_data/feedfill/jutarnji.txt"
feed="http://www.jutarnji.hr/rss?type=section&id=10"

echo ./extract.py $url $file $feed
./extract.py $url $file $feed
