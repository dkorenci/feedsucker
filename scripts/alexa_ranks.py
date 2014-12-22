'''
reads a list of urls from stdin and writes a list
of fetched alexa rank data for these urls to stdout
'''

import urllib2
import sys
import time
from scrapy.selector import Selector

class AlexaRankData:
    def __init__(self):
        self.global_rank = -1
        self.country_code = ""
        self.country_rank = -1

def fetchAlexaData(url):    
    requestUrl = "http://data.alexa.com/data?cli=10&url={0}".format(url)        
    response = urllib2.urlopen(requestUrl).read()
    selector = Selector(text=response, type="xml")    
    # extract country rank
    result = selector.xpath('/ALEXA/SD/COUNTRY/@RANK').extract()
    data = AlexaRankData()
    if not result : data.country_rank = None # list is empty
    else : data.country_rank = int(result[0])
    # extract global rank
    result = selector.xpath('/ALEXA/SD/COUNTRY/@CODE').extract()
    if not result : data.county_code = None
    else : data.country_code = result[0]   
    # extract global rank
    result = selector.xpath('/ALEXA/SD/POPULARITY/@TEXT').extract()
    if not result : data.global_rank = None
    else : data.global_rank = int(result[0])
    
    return data

def cempty(d,e = "NULL"): # converty empty (None or "") data to predefinded string
    if d == None or d == "" : return e
    else : return d
    

delay = 250 * 0.001 # delay, miliseconds converted to seconds
while True:        
    url = sys.stdin.readline().strip()
    if url == "" : break
    data = fetchAlexaData(url)
    print url, cempty(data.global_rank), cempty(data.country_code), cempty(data.country_rank)
    time.sleep(delay)
                