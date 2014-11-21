package rsssucker.core;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.persistence.EntityManagerFactory;
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
import rsssucker.data.mediadef.MediadefEntity;
import rsssucker.data.mediadef.MediadefException;
import rsssucker.data.mediadef.MediadefParser;
import rsssucker.data.mediadef.MediadefPersister;
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
    private EntityManagerFactory emf;
    
    private static final Logger errLogger = 
            LoggersManager.getErrorLogger(RssSuckerRunner.class.getName());
    private static final Logger infoLogger = 
            LoggersManager.getInfoLogger(RssSuckerRunner.class.getName());    
   
    public static void main(String[] args) {   
        RssSuckerApp app = new RssSuckerApp();
        app.run();
    }
    
    public void run() {
        try {
            initialize();
            mainLoop();
        } 
        finally {
            cleanup();
        }
        
    }

    private void initialize() {
        boolean result; terminate = false;
        result = readProperties(); if (result == false) terminate = true;
        result = processMediaDef(); if (result == false) terminate = true;
        result = initEntityManagerFactory(); if (result == false) terminate = true;
    }
    
    private void cleanup() {
        try {
            emf.close();
        }
        catch (Exception e) {
            errLogger.log(Level.SEVERE, "failed to cleanup", e);             
        }
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
        String mediadefFile = properties.getProperty("mediadef_file");
        MediadefParser parser; List<MediadefEntity> entities;        
        try { // parse mediadef file                        
            parser = new MediadefParser(mediadefFile);
            entities = parser.parse();
        }
        catch (IOException|MediadefException e) { 
            errLogger.log(Level.SEVERE, "failed to parse mediadef_file", e);            
            return false;
        }
        try { // persist entities from mediadef file
            new MediadefPersister().persist(entities);        
        }
        catch (MediadefException e) {
            errLogger.log(Level.SEVERE, "failed to persist mediadef_file", e);            
            return false;
        }
        return true;
    }    
    
    private boolean initEntityManagerFactory() {
        try {
            emf = Factory.createEmf();
        }
        catch (Exception e) {
            errLogger.log(Level.SEVERE, "failed to create EntityManagerFactory", e);            
            return false;            
        }
        return true;
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

    // initialize threads and jobs, run them and wait for them to finish
    private void doFeedRefresh() {
        List<Feed> feeds = getFeeds();
    }
    
    private List<Feed> getFeeds() {
        
    }
    
    private void initNewspaper() {
        try {
            newspaper = new Newspaper();
        } catch (IOException ex) {
            errLogger.log(Level.SEVERE, "failed to initialize Newspaper.", ex);
            newspaper = null;
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
