package rsssucker.core;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import rsssucker.RssSucker;
import rsssucker.article.ArticleData;
import rsssucker.article.newspaper.Newspaper;
import rsssucker.article.newspaper.NewspaperException;
import rsssucker.article.newspaper.NewspaperOutput;
import rsssucker.config.PropertiesReader;
import rsssucker.config.RssConfig;
import rsssucker.data.Factory;
import rsssucker.data.JpaContext;
import rsssucker.data.entity.Feed;
import rsssucker.data.entity.FeedArticle;
import rsssucker.feeds.FeedEntry;
import rsssucker.feeds.FeedReader;
import rsssucker.log.LoggersManager;
import rsssucker.util.Timer;

/**
 * Initialization and workflow the the application.
 */
public class RssSuckerApp {

    private FeedReader feedReader;
    private Newspaper newspaper;
    private boolean terminate = false;
    private Date lastFeedRefresh;
    // feeds refresh interval, in minutes
    private int refreshInterval;
    // main thread sleep period, in miliseconds
    private int sleepPeriod;
    private PropertiesReader properties;
    
    private static final Logger errLogger = LoggersManager.getErrorLogger(RssSuckerRunner.class.getName());
    private static final Logger infoLogger = LoggersManager.getInfoLogger(RssSuckerRunner.class.getName());
    
    public RssSuckerApp() {
        
    }
    
    public static void main(String[] args) {   
        RssSuckerApp app = new RssSuckerApp();
        app.run();
    }
    
    public void run() {
        initialize();
        mainLoop();
    }

    private void initialize() {
        if (!readProperties()) terminate = true;
        if (!processMediaDef()) terminate = true;
    }
    
    private boolean readProperties() {
        try {
            properties = new PropertiesReader(RssConfig.propertiesFile);
            refreshInterval = Integer.parseInt(properties.getProperty("refresh_interval"));
            // set sleepPeriod to 1/100 th of refresh interval (in miliseconds)
            sleepPeriod = refreshInterval * 60 * 1000 / 100;
            return true;
        } catch (IOException ex) {
            errLogger.log(Level.SEVERE, 
                    "failed to read properties.", ex); 
            return false;
        } catch (NumberFormatException ex) {
            errLogger.log(Level.SEVERE, 
                    "failed to parse refresh_interval value.", ex);             
            return false;
        }         
    }
    
    // read feeds and outlets from mediadef file, 
    // and create corresponding entities in the database 
    private boolean processMediaDef() {
        
        return false;
    }    
    
    private void singleThreadJob(Feed feed) {
        // reader = getFeedReader()
        // list<entries> entries = reader.read(feed.url)
        // entries = filterEntries(entries)
        // scraper = getArticleScraper()
        // foreach entry : entries
        //       articleData = scraper.scrape(entry.url)
        //       saveData(feed, entry, articleData)
        //
        // saveData - zapiši ako nema, dodaj u cache
    }
    
    private void mainLoop() {
        boolean sleep = false;
        while (!terminate) {
            if (sleep) {
                try {
                    Thread.sleep(sleepPeriod);
                } catch (InterruptedException ex) {
                   infoLogger.log(Level.SEVERE, "main thread sleep interrupted.", ex);
                }
                sleep = false;
            }            
            if (lastFeedRefresh == null) { // first entry, no last refresh
                doFeedRefresh();
                sleep = true;
            }
            else { // not first entry, check if refresh time has come
                Date now = new Date();
                double diffMin = getDifferenceInMinutes(lastFeedRefresh, now);
                if (diffMin > refreshInterval) {
                    doFeedRefresh();
                }
                sleep = true;
            }
        }
    }

    private void doFeedRefresh() {
        infoLogger.log(Level.INFO, "starting feed refresh");
        Timer timer = new Timer();
        List<FeedEntry> entries = feedReader.getNewFeedEntries();
        infoLogger.log(Level.INFO, "time to fetch new entries: " + timer.fromStart());        
        int saved = 0, saveFailed = 0;
        for (FeedEntry e : entries) {
            ArticleData news;
            try { 
                Timer t = new Timer();
                news = newspaper.scrapeArticle(e.getArticleURL());
                infoLogger.log(Level.INFO, "time to newspaper: " + 
                        t.fromStart() + " for article: " + e.getArticleURL());
            } catch (Exception ex) {
                errLogger.log(Level.SEVERE, "failed to process URL: " + e.getArticleURL(), ex);
                if ( (ex instanceof NewspaperException) == false) {
                    // severe exception occured, not just error reported by newspaper
                    // restart newspaper
                    errLogger.log(Level.SEVERE, "restaring newspaper");
                    newspaper.close();
                    initNewspaper();
                    if (newspaper == null) {
                        errLogger.log(Level.SEVERE, 
                                "terminating because of failure to initialize Newspaper");
                        terminate = true; break;
                    }                    
                }
                continue;
            }
            boolean success = saveArticle(e, news);
            if (success) saved++; else saveFailed++;            
        }
        infoLogger.log(Level.INFO, "ending feed refresh, it lasted " + timer.fromStart());
        infoLogger.log(Level.INFO, "number of new entries: " + entries.size());
        infoLogger.log(Level.INFO, "number of saved entries: " + saved);
        infoLogger.log(Level.INFO, "number of entries failed to save: " + saveFailed);
        // set last refresh to now
        lastFeedRefresh = new Date();
    }

    private void initNewspaper() {
        try {
            newspaper = new Newspaper();
        } catch (IOException ex) {
            errLogger.log(Level.SEVERE, "failed to initialize Newspaper.", ex);
            newspaper = null;
        }
    }
    
    // persist article to database
    private boolean saveArticle(FeedEntry feedEntry, ArticleData news) {
        // copy data
        FeedArticle article = new FeedArticle();        
        article.setDatePublished(feedEntry.getDate());
        article.setExtractedTitle(news.getTitle());
        article.setFeedTitle(feedEntry.getTitle());
        article.setDescription(feedEntry.getDescription());
        article.setText(news.getText());
        article.setUrl(feedEntry.getArticleURL());
        JpaContext ctx = null;
        try {
            // open transaction context
            ctx = Factory.createContext();        
            // save, commit, close
            ctx.beginTransaction();
            ctx.em.persist(article);        
            ctx.commitTransaction();            
            infoLogger.log(Level.INFO, "article successfuly saved: " + article.getUrl());
            return true;
        } catch (Exception e) {
            if (!isCommonError(e))
                errLogger.log(Level.SEVERE, "saving article failed " + article.getUrl(), e);
            return false;
        }      
        finally {            
            try {
                if (ctx != null) ctx.close();            
            }
            catch (Exception e) {
                errLogger.log(Level.SEVERE, "closing JpaContext failed", e);
            }
        }
               
    }    
    
    // return true if Exception describes a common situation that
    // is not alarming and should not be logged
    private boolean isCommonError(Exception e) {
        if (duplicateUrlError(e)) return true;
        else return false;
    }
    
    // unable to write NewsArticle because article with the same key exists
    // this can occur if one article is present in more than one feed, 
    // or if it is updated and put in a feed several times
    private boolean duplicateUrlError(Exception e) {
        Throwable thr = e;
        // unwind cause stack to get root cause
        while (thr.getCause() != null) thr = thr.getCause();
        String message = thr.getMessage();
        System.out.println(message);
        if (message.contains("duplicate key value violates unique constraint"))
            return true;
        else return false;
    }
    
    // return absolute value difference of two time points, in minutes
    private double getDifferenceInMinutes(Date date1, Date date2) {
        final int milisPerMinute = 60*1000;
        double diff = Math.abs(date2.getTime() - date1.getTime());
        return diff/milisPerMinute;
    }
    
}
