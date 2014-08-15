import newspaper
from newspaper import Article
from newspaper import Config
import sys

def printArticleText(articleURL):
    #conf = Config()
    #conf.fetch_images = False    
    try:
        article = Article(url=articleURL)
        article.download()
        article.parse()       
        title = article.title.encode('ascii', 'ignore');
        text = article.text.encode('ascii', 'ignore');
        print title
        print "!-TITLE-END-!"
        print text
        print ""
        print "!-END-!"
        sys.stdout.flush()
    except :
        for mess in sys.exc_info():
            print mess 
        print "[ERR]"    
        print ""
        print "!-END-!"
        sys.stdout.flush() 

def run():        
    while True:        
        line = sys.stdin.readline().strip()
        line = unicode(line, "utf-8")
#         file = open("test1.txt", "w") 
#         file.write(line+"\n")
#         file.close()
        if line == "EXIT" : break
        else : printArticleText(line)        

#printArticleText("http://www.theguardian.com/environment/2014/jul/28/bee-research-funding-pesticides-mps")    
run()    
        
