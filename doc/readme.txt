Feedsucker is an application for collecting texts
from a set of feeds and storing them in a relational database. 
Its functionality is oriented towards the main use-case - 
collecting texts of news articles from feeds of news outlets. 
It is a free software, licensed under the Apache License, Version 2.0

Supported feeds are rss and atom feeds and a class of html pages 
(containing URLs in with specific structure common for news sites). 
For text scraping, feedsucker relies on newspaper, a tool written in python.
Feedsucker is extensible, new scrapers (IArticleScraper classes) 
and feed readers (IFeedReader classes) can be written and plugged into app workflow. 

Documentation is contained in the following folders:
doc/structure/ : Structure of the main application code, and supporting functionality. 
doc/deploy/ : Deployment instructons
doc/todo/ : List of functionality to add, todo lists and bug lists. 


