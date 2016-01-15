#!/usr/bin/python
#-*-coding:utf-8-*-

'''
Extract links of interest from html page.
'''

from lxml import etree
from urlparse import urlparse

import re, sys, codecs

CRO_CHARS = u'[a-zA-ZćčđšžĆČĐŠŽ]'

REGEXES = {
    'www.jutarnji.hr':
        [ur'http://www\.jutarnji\.hr/({0}+\-+)+({0}+\-*)/[0-9]+/'.format(CRO_CHARS)]
}

FILTERS = {
    'www.jutarnji.hr': [ur'.*/jl\-footer']
}

def extractLinks(url):
    # compile link selection regexes
    up = urlparse(url); address = up.netloc
    regexes = [re.compile(regex, re.UNICODE) for regex in REGEXES[address]]
    filters = [re.compile(regex, re.UNICODE) for regex in FILTERS[address]]
    accept = lambda l: urlMatches(l, regexes, filters)
    # download all links
    links = getLinks(url)
    return [ unicode(l) for l in links if accept(l) ]

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
    return [e for e in root.xpath('//a/@href')]

def extractToFile(pageUrl, file, feedUrl):
    print 'extracting %s\nto file %s\nfeed:%s' % (pageUrl, file, feedUrl)
    links = extractLinks(pageUrl)
    f = codecs.open(file, 'w', 'utf-8')
    f.write(feedUrl+'\n')
    for url in links:
        f.write(url+'\n')

if __name__ == '__main__':
    args = sys.argv
    if len(args) < 4:
        print 'needed 3 parameters: url of page to extract links from,\n' \
              'file to save links, url of feed from rsssucker database'
    else:
        extractToFile(args[1], args[2], args[3])

#extractLinks('http://www.jutarnji.hr/')
