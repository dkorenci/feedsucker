import pandas.io.parsers as parsers

def readTable(fileName):
    table = parsers.read_table(fileName , quotechar='"')
    return table

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

ERRURL = '../tmp/errorUrlAll_topus05022015.txt'
TABLE = '../tmp/table_topus03022015.txt'

errUrl = readLines(ERRURL)
table = readTable(TABLE)

includedErrUrl(errUrl, table)
    

