Feedsucker is an application for collecting texts from 
a set of feeds over a period of time and storing them in a relational database. 
Main use case is collection of texts from web media outlets. 
Feedsucker is a free software, licensed under the Apache License, Version 2.0

Documentation is contained in the following folders:
doc/structure/ : Structure of the main application code, and supporting functionality. 
doc/deploy/ : Deployment instructons
doc/todo/ : List of functionality to add, todo lists and bug lists. 

Feedsucker is written in Java (Java 7 or above is required), with interface to newspaper 
written in Python and with control and support functionality written in Bash.
For this reason, fastest way to deploy it is to use a Linux system 
(the alternative is to convert a couple of most important scripts 
to scripting languages for other platforms).

Supported feeds are rss and atom feeds and a class of html pages 
(containing URLs with specific structure common for news sites). 
For text scraping, feedsucker relies on newspaper, a tool written in Python.
In general, a feed can be viewed as any source of URLs or other addresses 
pointing to resources containing text that can be scraped/extracted.
Feedsucker is extensible and new scrapers (IArticleScraper classes) 
and feed readers (IFeedReader classes) can be written and used within app workflow.

Feedsucker was developed as a research tool for creating media text corpora.
It enables a researcher to collect texts from a set of outlets/feeds of interest.
Following research articles are based on data collected with Feedsucker: 
"Getting the Agenda Right: Measuring Media Agenda using Topic Models"
"Issues and their Salience in the 2015 Parliamentary Election in Croatia:
 A Topic Model based Analysis of the Media Agenda" (to appear)

Largest deployment up to date collected 1.1 million texts 
from 73 feeds (25 outlets) over a period of 14 months, 
but the app should easily handle at least hundreds of feeds.
