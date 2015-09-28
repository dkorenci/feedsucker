# -*- coding: utf-8 -*-

# command line interface for newspaper - news article data extractor
# read on url per line and prints article title, text and error messages

import newspaper
from newspaper import Article
from newspaper import Config
from newspaper import configuration
import sys, time
from lxml.html.soupparser import fromstring

# encode string in utf-8 and print
def printUtf8(string):
	print string.encode('utf-8')

def getConf(language, parser):
    '''
    get newspaper Config object
    :return:
    '''
    conf = Config(); conf.fetch_images = False; conf.memoize_articles = False
    conf.set_language(language); conf.parser_class = parser
    return conf

def printArticleText(articleURL, language):
    conf = getConf(language, 'lxml')
    try:
        article = Article(url=articleURL, config=conf)
        try: #try with lxml
            article.download()
            article.parse()
        except: # error, try with beautifulsoup
            # save error message
            errorMessage = '\n'.join( [ str(mess) for mess in sys.exc_info() ] )
            conf = getConf(language, 'soup')
            article = Article(url=articleURL, config=conf)
            time.sleep(0.25) # pause before download, not to bombard server with subsequent request
            try:
                article.download()
                article.parse()
            except: # print lxml error message and exit
                printUtf8(errorMessage)
                printUtf8("[ERR]")
                printUtf8("")
                printUtf8("!-END-!")
                sys.stdout.flush()
                return
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
            printUtf8(str(mess))
        printUtf8("[ERR]")
        printUtf8("")
        printUtf8("!-END-!")
        sys.stdout.flush()

def run():        
    # read configuration lines
    # language code
    language = sys.stdin.readline().strip()
    while True:        
        line = sys.stdin.readline().strip()
        line = unicode(line, "utf-8")
        if line == "EXIT" : break
        else : printArticleText(line, language)        

#printArticleText('http://www.theguardian.com/environment/2014/jul/28/bee-research-funding-pesticides-mps', 'en')
#printArticleText('http://www.novilist.hr/Vijesti/Svijet/Vucic-Ako-u-iducih-30-sati-ne-bude-postignut-dogovor-s-Hrvatskom-o-potpunom-otvaranju-Bajakova-primijenit-cemo-mjere', 'hr')
run()
        
