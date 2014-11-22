package rsssucker.core;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Query;
import rsssucker.article.IArticleScraper;
import rsssucker.article.newspaper.Newspaper;
import rsssucker.config.PropertiesReader;
import rsssucker.config.RssConfig;
import rsssucker.core.feedprocessor.FeedProcessor;
import rsssucker.data.Factory;
import rsssucker.data.entity.Feed;
import rsssucker.data.mediadef.MediadefEntity;
import rsssucker.data.mediadef.MediadefException;
import rsssucker.data.mediadef.MediadefParser;
import rsssucker.data.mediadef.MediadefPersister;
import rsssucker.feeds.FeedReader;
import rsssucker.feeds.IFeedReader;
import rsssucker.feeds.RomeFeedReader;
import rsssucker.log.LoggersManager;

/**
 * Initialization and workflow the the application.
 */
public class RssSuckerApp {

    private boolean terminate = false;
    private Date lastFeedRefresh;
    // feeds refresh interval, in minutes
    private int refreshInterval;
    // main thread sleep period, in miliseconds
    private int sleepPeriod;
    private PropertiesReader properties;    
    private EntityManagerFactory emf;
    private List<Newspaper> npapers;
    
    private static final Logger errLogger = 
            LoggersManager.getErrorLogger(RssSuckerRunner.class.getName());
    private static final Logger infoLogger = 
            LoggersManager.getInfoLogger(RssSuckerRunner.class.getName());   
    private static final int DEFAULT_NUM_THREADS = 10;
       
    
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
        try { emf.close(); }
        catch (Exception e) { logErr("failed to cleanup", e); }
    }
    
    private static void logErr(String msg, Exception e) {
        errLogger.log(Level.SEVERE, msg, e);        
    }
    
    // send info message
    private static void info(String msg) { 
        String header = String.format("[%1$td%1$tm%1$tY_%1$tH:%1$tm:%1$tS] : ", new Date());
        System.out.println(header + msg); 
    }
    
    private static void logInfo(String msg, Exception e) {
        infoLogger.log(Level.INFO, msg, e);        
    }    
    
    private boolean readProperties() {
        try {
            properties = new PropertiesReader(RssConfig.propertiesFile);
            refreshInterval = Integer.parseInt(properties.getProperty("refresh_interval"));
            // set sleepPeriod to 1/100 th of refresh interval (in miliseconds)
            sleepPeriod = refreshInterval * 60 * 1000 / 100;
            return true;
        } catch (IOException ex) { 
            logErr("failed to read properties.", ex);
            return false; 
        } catch (NumberFormatException ex) {
            logErr("failed to parse refresh_interval value.", ex);             
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
            logErr("failed to parse mediadef_file", e);            
            return false;
        }
        try { // persist entities from mediadef file
            new MediadefPersister().persist(entities);        
        }
        catch (MediadefException e) {
            logErr("failed to persist mediadef_file", e);            
            return false;
        }
        return true;
    }    
    
    private boolean initEntityManagerFactory() {
        try {
            emf = Factory.createEmf();
        }
        catch (Exception e) {
            logErr("failed to create EntityManagerFactory", e);            
            return false;            
        }
        info("initialized EntityManagerFactory");
        return true;
    }  
   
    private void mainLoop() {
        boolean sleep = false;
        while (!terminate) {
            if (sleep) {
                try {
                    Thread.sleep(sleepPeriod);
                } catch (InterruptedException ex) {
                   logInfo("main thread sleep interrupted.", ex);
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

    // initialize threads and feed processing jobs, run them and wait for them to finish
    private void doFeedRefresh() { try {            
        info("starting feed refresh");
        List<Feed> feeds = getFeeds(); //feeds = feeds.subList(0, 1);
        int numThreads = getNumThreads();    
        if (feeds.size() < numThreads) numThreads = feeds.size();
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        for (Feed f : feeds) {
            IArticleScraper scraper = getNewspaper(); if (scraper == null) continue;
            IFeedReader reader = getFeedReader();            
            FeedProcessor processor = new FeedProcessor(f, emf, reader, scraper);
            info("starting processor for feed " + f.getUrl());
            executor.submit(processor);
        }
        executor.shutdown();
        // TODO it the executor is not terminated after a long time, do shutdownNow
        // how long? the use cases should be considered
        // responsivity also depends on FeedProcessor error and blocking handling
        info("waiting for FeedProcessor threads to finish");
        while (executor.isTerminated() == false) {
            try { executor.awaitTermination(refreshInterval, TimeUnit.MINUTES); }
            catch (InterruptedException e) {}           
        }
        info("FeedProcessor threads finished");
        closeNewspapers();
        lastFeedRefresh = new Date();
        info("ending feed refresh");
        
    }
    catch (Exception e) { logErr("feed refresh error", e); }
    }
    
    // read all feeds from the database
    private List<Feed> getFeeds() {
        EntityManager em = emf.createEntityManager();
        Query q = em.createNamedQuery("Feed.getAll");
        List<Feed> feeds = (List<Feed>)q.getResultList();
        return feeds;
    }
    
    // read number of threads from the properties file
    private int getNumThreads() {
        int nt;
        try { nt = Integer.parseInt(properties.getProperty("num_threads")); }
        catch (NumberFormatException|NullPointerException e) { nt = DEFAULT_NUM_THREADS; }
        if (nt <= 0) nt = DEFAULT_NUM_THREADS;
        return nt;
    }
    
    // create newspaper and cache for later shutdown
    private Newspaper getNewspaper() {
        if (npapers == null) npapers = new ArrayList<Newspaper>();
        Newspaper npaper = createNewspaper();
        if (npaper == null) return null;
        else { 
            npapers.add(npaper);
            return npaper;
        }
    }
    
    private void closeNewspapers() {
        for (Newspaper npaper : npapers) {
            try { npaper.close(); }
            catch (Exception ex) { logErr("closing newspaper error", ex); }
        }        
    }
    
    // initialize and return a new newspaper instance
    private Newspaper createNewspaper() {
        try {
            return new Newspaper();
        } catch (IOException ex) {
            errLogger.log(Level.SEVERE, "failed to initialize Newspaper.", ex);
            return null;
        }
    }
    
    private RomeFeedReader getFeedReader() { return new RomeFeedReader(); }
        
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
