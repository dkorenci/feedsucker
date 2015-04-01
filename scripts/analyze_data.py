import pandas.io.parsers as parsers
from fpconst import NaN
import datetime
from dateutil.parser import parse
import random
import matplotlib.pyplot as plt
from feedscraper.deploy.deploy import get_top_domain
import pandas
import matplotlib.dates as mdates
import matplotlib.cbook as cbook
import pandas.tslib as pts
from textwrap import wrap
from rpy2 import robjects

SEED = 88775621

def readTable(fileName):
    columnTypes = {'id':int, 'date_published':datetime, 'date_saved':datetime,
                  'url':str, 'author':str, 'title_scraped':str, 'title_feed':str,
                  'description':str, 'text':str } # does not seem to have effect
    table = parsers.read_table(fileName, quotechar='"', parse_dates = [2])
    table.replace(to_replace = {'text': {NaN:''}}, inplace=True) # empty string are read in as NaN, replace
    # date_published containes "null" values, so read_table wont parse the dates
    # code below seems to throw exceptions, see the docs
    newDatePub = table['date_published'].apply(lambda a : pandas.tslib.Timestamp(a) if a != 'null' else None)
    table['date_published'] = newDatePub
    return table

def printTextSample(table, sampleSize, fileName, filter):
    'print to the file a sample of texts from the table for which filter returns true'
    random.seed(SEED)
    texts = table['text']; urls = table['url']; titles = table['title_feed']
    # filter sample
    indexes = [ i for i in range(len(table)) if filter(texts[i]) ]
    f = open(fileName, "w")
    f.write("set size: " + str(len(indexes))+"\n")
    f.write("sample size: " + str(sampleSize) + "\n")
    f.write("seed: " + str(88775621) + "\n\n")
    random.shuffle(indexes)
    sampleSize = len(indexes) if sampleSize < 0 else sampleSize
    for i in indexes[:sampleSize] :
        f.write('URL: ' + urls[i]+"\n\n")
        title = titles[i]; title.replace('\n', ' ');
        f.write('TITLE: ' + title +"\n\n")
        text = texts[i];
        text.replace('\n', ' '); text = '\n'.join(wrap(text, 100));
        f.write(text); f.write("\n\n*****************\n");

def readLines(fileName):
    lines = []
    for line in open(fileName).readlines():
        lines.append(line[:-1])    
    return lines

# check how many error urls exist in the table
def includedErrUrl(errUrl, table):
    errset = set(errUrl)
    urlset = set(table['url'])
    diff = errset.difference(urlset)    
    print 'err urls: %d , not_saved: %d' % (len(errset), len(diff))
    print 'percentage not saved: %.3f ' % (float(len(diff)) / len(errset))
    
def analyzeNonEmptyUrls(errUrl, table):
    empty = 0; size = len(table['text'])
    for txt in table['text'] : 
        if type(txt) is float : empty = empty + 1
    print 'size: %d , empty: %d , percentage: %f' % (size, empty, float(empty)/size)
    
def printShortTexts(table, length):
    file = open("short.txt", "w")
    for txt in table['text'] : 
        if type(txt) is not float and len(txt) < length : 
            file.write(txt); file.write("\n\n ********* \n");

# tokenize by whitespace          
def numTokens(text):  
    return len(text.split())

# histogram of text length distribution, all lengths > maxLen
# are filterd out, number of histogram bins is specified
def plotLengthDist(table, lenFunc = lambda x : len(x),
                   maxLen = 1000000000, numBins = 200):
    fig, ax = plt.subplots()
    ll =  [ lenFunc(txt) for txt in table['text'] ]
    ll = [n for n in ll if n < maxLen]
    ax.hist(ll, bins=numBins)
    plt.show()

# 5 number summary text length distribution
def plotLengthSummary(table, lenFunc = lambda x : len(x)):
    ll = [ lenFunc(txt) for txt in table['text'] ]
    summary = robjects.r('summary')
    print summary(robjects.IntVector(ll))

# return table of rows with text length between specified values
def filterByNumTokens(table, minLen, maxLen):
    # get text positions as a series of bools
    pos = (table['text'].apply(numTokens)).between(minLen, maxLen)
    # convert series of bool to a list of indexes
    ind = [i[0] for i in pos.iteritems() if i[1] == True]
    return table.iloc[ind, :]    

# 110, 145
def sampleTextsFromLengthRange(table, sampleSize, minLen, maxLen):
    ltable = filterByNumTokens(table, minLen, maxLen)
    if (sampleSize < len(ltable)) : sampleSize = len(ltable)
    printTextSample(ltable, sampleSize, "small_sample.txt")
    
# for timestamp, return datetime all the data except year,month,day removed     
def extractDay(time):        
    return pandas.datetime(time.year, time.month, time.day)

def extractHour(time):        
    return pandas.datetime(time.year, time.month, time.day, time.hour)

def extractHalfDay(time):    
    return pandas.datetime(time.year, time.month, time.day)

def groupByTime(dates, timeProject):
    return dates.groupby(lambda i : timeProject(dates[i])).count()   

# return False if date is NaT or too small, 
def filterDate(date, minDate, maxDate):
    if (type(date) == pandas.tslib.NaTType) : return False
    elif (date < minDate) : return False
    elif (date > maxDate) : return False
    else : return True
    
# filter out NaT and random values
#minDate = '2014-12-24 00:00:01', maxDate = '2015-12-05 00:00:01'):    
#def filterDates(dates, minDate = '2015-02-06 00:00:01', maxDate = '2015-12-05 00:00:01'):
def filterDates(dates, minDate = '2014-12-24 00:00:01', maxDate = '2015-12-05 00:00:01'):
    minD, maxD = pts.Timestamp(minDate), pts.Timestamp(maxDate)    
    clean = (dates.apply(lambda x : filterDate(x, minD, maxD)))
    return dates[clean]

def plotDates(dates):
    dates = filterDates(dates)
    # aggregate data
    agg = groupByTime(dates, extractHour)    
    dates, counts = agg.index, agg.values
    # plot
    fig, ax = plt.subplots()
    #ax.plot_date(dates, counts)    
    ax.bar(dates, counts, width=0.5)
    ax.xaxis_date()
    months = mdates.MonthLocator()
    ax.xaxis.set_major_locator(months)
    dateForm = mdates.DateFormatter('%Y-%m   ')#'-%m-%d'
    ax.xaxis.set_major_formatter(dateForm)
    days = mdates.DayLocator()            
    ax.xaxis.set_minor_locator(days)
    dayForm = mdates.DateFormatter('%d')
    ax.xaxis.set_minor_formatter(dayForm)
    fig.autofmt_xdate()
    plt.show()

ERRURL = '/data/rsssucker_data/errorUrlAll_topus05022015.txt'
TABLE = '/datafast/rsssucker_data/table_topus_23032015_15:30.txt'
#TABLE = '/data/rsssucker_data/table_topus03022015.txt'
#TABLE='/data/rsssucker_data/table_loop_test_mediagg_23022015.txt'

#errUrl = readLines(ERRURL)
#table = readTable(TABLE)

#plotSavedDates(table)
#includedErrUrl(errUrl, table)
#analyzeNonEmptyUrls(errUrl, table)
#printShortTexts(table, 100)
#exploreLengthDist(table)



    

