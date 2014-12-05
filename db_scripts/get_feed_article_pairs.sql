SELECT feedarticle.url, feed.url
  FROM feedarticle
LEFT OUTER JOIN feedarticle_feed
  ON feedarticle.id = feedarticle_feed.articles_id
LEFT OUTER JOIN feed
  ON feedarticle_feed.feeds_id = feed.id 
