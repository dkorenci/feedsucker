package rsssucker.core.feedprocessor;

import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import rsssucker.article.ArticleData;
import rsssucker.article.IArticleScraper;
import rsssucker.core.RssSuckerApp;
import rsssucker.data.cache.Filter;
import rsssucker.data.entity.EntityFactory;
import rsssucker.data.entity.Feed;
import rsssucker.data.entity.FeedArticle;
import rsssucker.feeds.FeedEntry;
import rsssucker.feeds.IFeedReader;
import rsssucker.log.RssSuckerLogger;
import rsssucker.util.HttpUtils;

/**
 * Downloads entries from a single feed, scrapes the articles and save them to database.
 */
public class FeedProcessor implements Runnable {
    
    private final IFeedReader freader;
    private final IArticleScraper scraper;
    private final Feed feed;
    private final EntityManagerFactory emf;  
    private final Filter filter;    
    private final int feedPause, articlePause;    
    
    private class ScrapedFeedEntry {
        public FeedEntry feedEntry;
        public ArticleData scrapedData;
        
        
        public ScrapedFeedEntry(FeedEntry fe, ArticleData ad) { 
            feedEntry = fe; scrapedData = ad; 
        }
    }
    
    private List<ScrapedFeedEntry> entries;
    
    private static final RssSuckerLogger logger = 
            new RssSuckerLogger(FeedProcessor.class.getName());        
    
    public FeedProcessor(Feed f, EntityManagerFactory e, 
            IFeedReader fread, IArticleScraper scr, Filter fil, 
            int feedPause, int articlePause) {
        feed = f; freader = fread; scraper = scr; emf = e; filter = fil;
        this.feedPause = feedPause; this.articlePause = articlePause;        
    }

    @Override
    public void run() {
        boolean result;
        try {           
            checkInterrupted();
            result = readFeedEntries(); if (result == false) return;
            scrapeFeedArticles(); 
            saveFeedArticles();
        }
        catch (InterruptedException ex) {
            logger.info("feed processing interrupted occured for feed: " + feed.getUrl());
            logger.logInfo("feed processing interrupted for feed: " + feed.getUrl(), ex); 
        }
        catch (Exception ex) { 
            logger.logErr("feed processing error occured for feed: " + feed.getUrl(), ex); 
        }
    }

    private void checkInterrupted() throws InterruptedException {
        if (Thread.currentThread().isInterrupted()) 
            throw new InterruptedException();
    }
    
    // read feed entries, log error and return false if fail
    private boolean readFeedEntries() throws InterruptedException {        
        logger.info("start reading entries");
        try {                        
            List<FeedEntry> fentries = freader.getFeedEntries(feed.getUrl());    
            //fentries = fentries.subList(0, 5);
            entries = new ArrayList<>(fentries.size());
            for (FeedEntry e : fentries) { 
                // check if (feedUrl, articleUrl) pair already exists
                if (filter == null || !filter.contains(feed.getUrl(), e.getUrl())) {
                    if (Feed.TYPE_SYNDICATION.equals(feed.getType())) {
                        redirectUrl(e);  
                    }
                    entries.add(new ScrapedFeedEntry(e, null));
                } 
            }
        } catch (Exception ex) {
            logger.logErr("download feed entries for feed failed: " + feed.getUrl(), ex); 
            return false;
        }
        logger.info("finished reading entries");
        Thread.sleep(feedPause);                
        return true;
    }

    // find redirect location for entry's url, write it to redirUrl
    private void redirectUrl(FeedEntry e) {
        try {
            String host = new URI(e.getUrl()).getHost().trim();
            String redir = null;
            if ("news.google.com".equals(host)) 
                redir = HttpUtils.resolveGoogleRedirect(e.getUrl());
            else 
                redir = HttpUtils.resolveHttpRedirects(e.getUrl());
            e.setRedirUrl(redir);
        }
        catch (Exception ex) {
            String message;
            if (e == null) 
                message = "redirecting url failed. feed: "+feed.getUrl()+"\n"
                            +"FeedEntry is null";
            else 
                message = "redirecting url failed. feed: "+feed.getUrl()+"\n"
                            +"FeedEntry url: " + e.getUrl();                    
            logger.logErr(message, ex);
            e.setRedirUrl(e.getUrl());
        }
    }
    
    private void scrapeFeedArticles() throws InterruptedException {
        logger.info("start scraping");
        for (ScrapedFeedEntry entry : entries) {
            String url = entry.feedEntry.getRedirUrl();
            checkInterrupted();
            try {
                ArticleData data = scraper.scrapeArticle(url);
                entry.scrapedData = data;
            } catch (Exception ex) {
                logger.logErr("scraping article failed for : "
                    + url + " from feed: " + feed.getUrl(), ex);                
                entry.scrapedData = null;
            }            
            Thread.sleep(articlePause);
        }
        logger.info("finished scraping");
    }

    private void saveFeedArticles() throws InterruptedException {
        logger.info("start saving");        
        EntityManager em = null;
        try {
        
        em = emf.createEntityManager();            
        for (ScrapedFeedEntry e : entries) {
        checkInterrupted();
        if (e.scrapedData != null) { // check if scraping failed
            if (em.getTransaction().isActive()) {
                // this should not happen, since transaction should be 
                // commited or rolled back, continue with new entity manager
                try { em.close(); } catch (Exception ex) { logger.logErr("closing em failed", ex); }
                em = emf.createEntityManager();
            }
            try {
                em.getTransaction().begin();            
                Query q = em.createNamedQuery("FeedArticle.getByUrl");                
                q.setParameter("url", e.feedEntry.getRedirUrl());
                FeedArticle article;
                try { article = (FeedArticle)q.getSingleResult(); } // get managed article
                catch (NoResultException ex) { article = null; }
                if (article == null)  {
                    article = EntityFactory.createFeedArticle(e.feedEntry, e.scrapedData);
                    article.setDateSaved(new Date());
                    em.persist(article);
                } // else article could be refreshed with new data, if this is the policy                                              
                Feed f = em.find(Feed.class, feed.getId()); 
                if (f == null) throw new IllegalArgumentException("feed is not in the database");
                article.getFeeds().add(f);           
                if (article.getDatePublished() == null) {
                    article.setDatePublished(e.feedEntry.getDate());                    
                }
                // feed.getArticles().add(article); is this necessary? this would slow down things
                em.getTransaction().commit();    
                // save original, not redirected feed entry url to filter, 
                // since filtering happens before redirection
                if (filter != null) filter.addEntry(f.getUrl(), e.feedEntry.getUrl());
            }
            catch (Exception ex) {
                String url = e.feedEntry.getUrl();
                logger.logErr("saving article failed: " + url, ex);
                try { em.getTransaction().rollback(); }
                catch (Exception ex2) {
                    logger.logErr("committing rollback failed for article: "+ url, ex2);
                }
            }
        }
        }
        
        }        
        finally { if (em != null) em.close(); }
        logger.info("finished saving");
    }
    
}
