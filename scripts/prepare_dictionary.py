# -*- coding: utf-8 -*-

'''
Processing on Goran Iglay's croatian dictionary.
'''

import re, codecs

def processDictionary(inFile, outFile):
    '''
    Transform input dictionary to a list of lowercase ascii alphabetic chars, one word per line.
    '''
    replaceChars = { u'ć':u'c', u'č':u'c', u'š':u's', u'ž':u'z', u'đ':u'd',
                    u'\u65279':'', u'é':u'e', u'ő':'o' }
    cnt = -1; wcnt = 0; badwcnt = 0
    words = set()
    for line in codecs.open(inFile, 'r', 'utf-8'):
        wcnt += 1
        fword = line.split('\t')[0] # take the first word form
        fword = fword.lower().strip()
        for repc in replaceChars:
            fword = fword.replace(repc, replaceChars[repc])
        if len(fword) > 0:
            if not re.match('^[a-z]+$', fword):
                badwcnt += 1
                #print fword
                #print '[%s]'%fword[0], (fword[0]), wcnt
                #print
            else:
                words.add(fword)
        else: badwcnt += 1
        cnt -= 1
        if cnt == 0: break;
    # add individual characters
    for ch in u'aeiou':
        words.add(ch)
        print ch
    print '%d words, %d bad words' % (wcnt, badwcnt)
    outf = codecs.open(outFile, 'w', 'utf-8')
    for w in words:
        outf.write(w);
        outf.write('\n')


inFile = '/data/download/HR_Txt-601.txt'
outFile = 'croatian_wordlist.txt'

processDictionary(inFile, outFile)
