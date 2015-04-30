-- us_politics corpus:

-- us_politics corpus:

SELECT setseed(0.1);

SELECT id, feedtitle, text FROM feedarticle WHERE
(datepublished > '2015-01-26 00:00:00' OR
(datepublished IS NULL AND datesaved > '2015-01-26 00:00:00'))
AND
(id IN (SELECT DISTINCT art.id AS feed_id FROM feedarticle AS art
JOIN (
SELECT DISTINCT articles_id, feeds_id FROM feedarticle_feed WHERE feeds_id IN
    ( SELECT id FROM feed WHERE url IN ('http://time.com/politics/feed/','http://feeds.nbcnews.com/feeds/nbcpolitics','http://online.wsj.com/xml/rss/3_7087.xml','http://rss.nytimes.com/services/xml/rss/nyt/Politics.xml','http://www.cbsnews.com/latest/rss/politics','http://www.chron.com/news/politics/collectionRss/RD-Politics-National-heds-18603.php','http://www.theblaze.com/stories/category/politics/feed/','http://www.theguardian.com/us-news/us-politics/rss','http://www.ibtimes.com/rss/politics/us','http://www.cnbc.com/id/10000113/device/rss/rss.html','http://feeds.reuters.com/Reuters/PoliticsNews','http://feeds.foxnews.com/foxnews/politics','http://feeds.washingtonpost.com/rss/politics','http://www.sfgate.com/rss/feed/Politics-RSS-Feed-436.php','http://www.bloomberg.com/politics/feeds/site.xml','http://feeds.nydailynews.com/nydnrss/news/politics','http://feeds.feedburner.com/AtlanticPoliticsChannel?format=xml','http://rss.cnn.com/rss/cnn_allpolitics.rss','http://www.huffingtonpost.com/feeds/verticals/politics/index.xml','http://www.chron.com/rss/feed/Politics-275.php') )
) AS t ON art.id = t.articles_id))
AND 
text ~*  '.*tortur.*' -- '.*mental\s+health.*' --'.*chapel\s+hill.*' --OR feedtitle ~* '.*mitt[:space:]+romney.*'
 ORDER BY random()