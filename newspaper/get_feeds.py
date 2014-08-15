import newspaper
from newspaper import Config

def getFeeds(url):
    config = Config()
    config.fetch_images = False
    paper = newspaper.build(url, config)
    for feed_url in paper.feed_urls(): # paper.article_urls(): #
        print feed_url
        
urls = ['http://h-alter.org', 'http://www.theguardian.com/uk', 'http://edition.cnn.com/services/rss/index.html', 'http://time.com/']

for url in urls: 
    print url
    print 'feeds: '
    getFeeds(url)