# command line interface for newspaper - news article data extractor
# read on url per line and prints article title, text and error messages

import newspaper
from newspaper import Article
from newspaper import Config
from newspaper import configuration
import sys

# encode string in utf-8 and print
def printUtf8(string):
	print string.encode('utf-8')

def printArticleText(articleURL):
    conf = Config()
    conf.fetch_images = False
    conf.memoize_articles = False    
    try:
        article = Article(url=articleURL, config=conf)
        article.download()
        article.parse()       
        title = article.title
        text = article.text
        printUtf8(title)
        printUtf8("!-TITLE-END-!")
        printUtf8(text)
        printUtf8("")
        printUtf8("!-END-!")
        sys.stdout.flush()
    except :
        for mess in sys.exc_info():
            printUtf8(mess)
        printUtf8("[ERR]")
        printUtf8("")
        printUtf8("!-END-!")
        sys.stdout.flush() 

def run():        
    while True:        
        line = sys.stdin.readline().strip()
        line = unicode(line, "utf-8")
        if line == "EXIT" : break
        else : printArticleText(line)        

#printArticleText("http://www.theguardian.com/environment/2014/jul/28/bee-research-funding-pesticides-mps")    
run()    
        
