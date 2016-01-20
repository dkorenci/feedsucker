#!/usr/bin/python
#-*-coding:utf-8-*-

'''
Extract links of interest from html page.
'''

from lxml import etree
from urlparse import urlparse

import re, sys, codecs

CRO_CHARS = u'[a-zA-ZćčđšžĆČĐŠŽ]'
CRO_CHARS_NUMS = u'[a-zA-ZćčđšžĆČĐŠŽ0-9]'

sdtempl=ur'/%s/.*'

REGEXES = {
    'www.jutarnji.hr':
        [ur'http://www\.jutarnji\.hr/\-*({0}+\-+)+({0}+\-*)/[0-9]+/'.format(CRO_CHARS_NUMS)],
    'http://www.jutarnji.hr/rss?type=section&id=10':
        [ur'http://www\.jutarnji\.hr/\-*({0}+\-+)+({0}+\-*)/[0-9]+/'.format(CRO_CHARS_NUMS)],
    'www.vecernji.hr':
        [ur'/hrvatska/.*\-104[0-9]+'], #({0}+\-)+({0}+)\-[0-9]+/'.format(CRO_CHARS_NUMS)]
    'http://www.vecernji.hr/hrvatska/':
        [ur'/hrvatska/.*\-104[0-9]+'],
    'http://www.vecernji.hr/zg-vijesti/':
        [ur'/zg-vijesti/.*\-104[0-9]+'],
    'http://www.slobodnadalmacija.hr/RssHrvatska.aspx':
        [ur'/Hrvatska/.*'],
    'http://www.slobodnadalmacija.hr/RssDalmacija.aspx':
        [sdtempl%'Split',sdtempl%u'Split-županija',sdtempl%'Dubrovnik',
         sdtempl%'Zadar',sdtempl%u'Šibenik'],
    'http://www.glas-slavonije.hr/Rss/Novosti':
        [ur'.*/[0-9]+/1/.*', ur'.+' ],
    'http://www.vijesti.rtl.hr/novosti/hrvatska/':
        [ur'http://www\.vijesti\.rtl\.hr/novosti/hrvatska/18[0-9]+/.*'],
    'http://www.tportal.hr/vijesti/hrvatska/':
        [ur'/vijesti/hrvatska/.+',
         ur'http://www\.tportal\.hr/vijesti/hrvatska/.+']
    #
}

FILTERS = {
    'www.jutarnji.hr': [ur'.*/jl\-footer'],
    'http://www.jutarnji.hr/rss?type=section&id=10': [ur'.*/jl\-footer'],
    'www.vecernji.hr': [],
    'http://www.vecernji.hr/hrvatska/':[],
    'http://www.vecernji.hr/zg-vijesti/':[],
    'http://www.slobodnadalmacija.hr/RssHrvatska.aspx':[],
    'http://www.slobodnadalmacija.hr/RssDalmacija.aspx':[],
    'http://www.glas-slavonije.hr/Rss/Novosti':[],
    'http://www.vijesti.rtl.hr/novosti/hrvatska/':[],
    'http://www.tportal.hr/vijesti/hrvatska/':[]
}

def extractLinks(url, feed):
    # compile link selection regexes
    #up = urlparse(url); address = up.netloc
    regexes = [re.compile(regex, re.UNICODE) for regex in REGEXES[feed]]
    filters = [re.compile(regex, re.UNICODE) for regex in FILTERS[feed]]
    accept = lambda l: urlMatches(l, regexes, filters)
    # download all links
    links = getLinks(url)
    return [ unicode(l) for l in links if accept(l) ], \
            [ unicode(l) for l in links if not accept(l) ]

    #return set( unicode(l) for l in links if accept(l) ), \
    #        set( unicode(l) for l in links if not accept(l) )

def urlMatches(url, regexes, filters):
    for r in filters:
        if r.match(url): return False
    for r in regexes:
        if r.match(url): return True
    return False

def getLinks(url):
    parser = etree.HTMLParser()
    tree = etree.parse(source=url, parser=parser)
    root = tree.getroot()
    #print etree.tostring(root)
    #for e in root.xpath('//a/@href'): print e
    return [e for e in root.xpath('//a/@href')]

def extractToFile(pageUrl, file, feedUrl, date):
    print 'extracting %s\nto file %s\nfeed:%s' % (pageUrl, file, feedUrl)
    links, missed = extractLinks(pageUrl, feedUrl)
    f = codecs.open(file, 'w', 'utf-8')
    f.write(feedUrl+'\n')
    f.write('[date] %s\n'%date)
    for url in links: f.write(url+'\n')
    m = codecs.open(file+'.missed', 'w', 'utf-8')
    for url in missed: m.write(url+'\n')

if __name__ == '__main__':
    args = sys.argv
    if len(args) < 4:
        print 'needed 3 parameters: url of page to extract links from,\n' \
              'file to save links, url of feed from rsssucker database'
    else:
        if len(args) == 5: date = args[4]
        else: date = '[unknown]'
        extractToFile(pageUrl=args[1], file=args[2], feedUrl=args[3], date=date)

#extractLinks('http://www.jutarnji.hr/')
