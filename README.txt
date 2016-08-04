Feedsucker is an application for collecting texts
from a set of feeds and storing them in a relational database. 
Its functionality is oriented towards the main use-case - 
collecting texts of news articles from feeds of news outlets. 
It is a free software, licensed under the Apache License, Version 2.0

Supported feeds are rss and atom feeds and a class of html pages 
(containing URLs with specific structure common for news sites). 
For text scraping, feedsucker relies on newspaper, a tool written in Python.
In general, feed can be viewed as any source of URLs or other addresses 
pointing to resources containing text that can be "scraped" or extracted.
So Feedsucker is extensible and new scrapers (IArticleScraper classes) 
and feed readers (IFeedReader classes) can be written and used within app workflow.

Documentation is contained in the following folders:
doc/structure/ : Structure of the main application code, and supporting functionality. 
doc/deploy/ : Deployment instructons
doc/todo/ : List of functionality to add, todo lists and bug lists. 

Feedsucker is written in Java, with interface to newspaper 
written in Python and some control and support functionality written in Bash.
For this reason, for now quick deployment (without modifications) 
is possible only on Linux/Unix systems, until Bash functionality 
is replaced by command line or GUI tools written in Java. 
