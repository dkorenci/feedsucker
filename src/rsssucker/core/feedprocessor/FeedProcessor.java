package rsssucker.core.feedprocessor;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Query;
import rsssucker.article.ArticleData;
import rsssucker.article.IArticleScraper;
import rsssucker.core.RssSuckerRunner;
import rsssucker.data.entity.EntityFactory;
import rsssucker.data.entity.Feed;
import rsssucker.data.entity.FeedArticle;
import rsssucker.feeds.FeedEntry;
import rsssucker.feeds.IFeedReader;
import rsssucker.log.LoggersManager;

/**
 * Downloads entries from a single feed, scrapes the articles and save them to database.
 */
public class FeedProcessor implements Runnable {
    
    private final IFeedReader freader;
    private final IArticleScraper scraper;
    private final Feed feed;
    private final EntityManagerFactory emf;
    private EntityManager em; 
    
    private class ScrapedFeedEntry {
        public FeedEntry feedEntry;
        public ArticleData scrapedData;
    }
    
    private List<ScrapedFeedEntry> entries;
    
    private static final Logger errLogger = 
            LoggersManager.getErrorLogger(RssSuckerRunner.class.getName());
    private static final Logger infoLogger = 
            LoggersManager.getInfoLogger(RssSuckerRunner.class.getName());    
        
    public FeedProcessor(Feed f, EntityManagerFactory e, 
            IFeedReader fread, IArticleScraper scr) {
        feed = f; freader = fread; scraper = scr; emf = e;
    }

    @Override
    public void run() {
        boolean result;
        try {
            em = emf.createEntityManager();
            em.merge(feed);
            result = readFeedEntries(); if (result == false) return;
            scrapeFeedArticles();
            saveFeedArticles();
        }
        finally {
            em.close();
        }
    }

    // read feed entries, log error and return false if fail
    private boolean readFeedEntries() {
        try {
            entries = new ArrayList<>();
            freader.getFeedEntries(feed.getUrl());                        
        } catch (Exception ex) {
            errLogger.log(Level.SEVERE, "failed to download feed entries for feed: "
                    + feed.getUrl(), ex);
            return false;
        }
        return true;
    }

    private void scrapeFeedArticles() {
        for (ScrapedFeedEntry entry : entries) {
            String url = entry.feedEntry.getArticleURL();
            try {
                ArticleData data = scraper.scrapeArticle(url);
                entry.scrapedData = data;
            } catch (Exception ex) {
                errLogger.log(Level.SEVERE, "failed to scrape article: "
                    + url + " from feed: " + feed.getUrl(), ex);                
                entry.scrapedData = null;
            }
        }
    }

    private void saveFeedArticles() {
        for (ScrapedFeedEntry e : entries) {
            Query q = em.createNamedQuery("FeedArticle.getByUrl");
            FeedArticle article = (FeedArticle)q.getSingleResult();
            em.getTransaction().begin();
            if (article == null)  {
                article = EntityFactory.createFeedArticle(e.feedEntry, e.scrapedData);
                em.persist(article);
            } // else article could be refreshed with new data, if this is the policy                        
            // this will be slow because of loading many articles from the DB, speedup
            // loading of articles for the feed has to be lazy!
            // solutions: write directly to the article-feed join table, or write only to articles            
            article.getFeeds().add(feed);
            em.getTransaction().commit();
            // feed.getArticles().add(article); is this necessary?
        }
    }
    
}
