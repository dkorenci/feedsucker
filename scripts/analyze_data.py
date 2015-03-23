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

def printTextSample(table, sampleSize, fileName):
    indexes = range(len(table))
    random.shuffle(indexes)    
    f = open(fileName, "w")
    texts = table['text']; urls = table['url']    
    for i in indexes[:sampleSize] :
        f.write(urls[table.index[i]]+"\n") 
        f.write(texts[table.index[i]]); f.write("\n\n ***************** \n\n");

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

def textLengthDist(table):
     return [ numTokens(txt) for txt in table['text'] ]

# histogram of text length distribution, all lengths > maxLen
# are filterd out, number of histogram bins is specified
def plotLengthDist(table, maxLen, numBins, binSetup):
    ll = textLengthDist(table)
    #plot.boxplot(ll)    
    ll = [n for n in ll if n < maxLen]
    plt.hist(ll, bins=numBins)

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
TABLE = '/data/rsssucker_data/table_topus09032015.txt'
#TABLE = '/data/rsssucker_data/table_topus03022015.txt'
#TABLE='/data/rsssucker_data/table_loop_test_mediagg_23022015.txt'

#errUrl = readLines(ERRURL)
#table = readTable(TABLE)

#plotSavedDates(table)
#includedErrUrl(errUrl, table)
#analyzeNonEmptyUrls(errUrl, table)
#printShortTexts(table, 100)
#exploreLengthDist(table)



    

