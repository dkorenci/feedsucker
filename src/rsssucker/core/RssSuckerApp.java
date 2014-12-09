package rsssucker.core;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Query;
import rsssucker.article.IArticleScraper;
import rsssucker.article.newspaper.RessurectingNewspaper;
import rsssucker.config.PropertiesReader;
import rsssucker.config.RssConfig;
import rsssucker.core.feedprocessor.FeedProcessor;
import rsssucker.core.messages.FinishAndShutdownException;
import rsssucker.core.messages.Messages;
import rsssucker.core.messages.ShutdownException;
import rsssucker.core.messages.ShutdownMonitor;
import rsssucker.data.Factory;
import rsssucker.data.cache.Filter;
import rsssucker.data.entity.Feed;
import rsssucker.data.mediadef.MediadefEntity;
import rsssucker.data.mediadef.MediadefException;
import rsssucker.data.mediadef.MediadefParser;
import rsssucker.data.mediadef.MediadefPersister;
import rsssucker.feeds.IFeedReader;
import rsssucker.feeds.RomeFeedReader;
import rsssucker.log.LoggersManager;
import rsssucker.log.RssSuckerLogger;

/**
 * Initialization and workflow the the application.
 */
public class RssSuckerApp {
    
    private Date lastFeedRefresh;
    // feeds refresh interval, in minutes
    private int refreshInterval;
    // main thread sleep period, in miliseconds
    private int sleepPeriod;
    private PropertiesReader properties;    
    private EntityManagerFactory emf;
    private List<RessurectingNewspaper> npapers;
    private Queue<String> messageQueue;
    private ShutdownMonitor shutdownMonitor;
    private Filter filter;
    private List<Feed> feeds;    
    
    private static final RssSuckerLogger logger = 
            new RssSuckerLogger(RssSuckerApp.class.getName());
    
    private int numThreads; 
    private int numNpapers, npaperPointer;    
    private static final int DEFAULT_NUM_THREADS = 10;
    private static final int DEFAULT_NUM_NPAPERS = 10;
    private static final int DEFAULT_REFRESH_INT = 30;
       
    // main loop (wait for next refresh) sleep period, in milis
    private static final int MAIN_LOOP_SLEEP = 1000;
    // how long to sleep when waiting for FeedProcessor threads to finish
    private static final int FEED_REFRESH_SLEEP = 500;
    // if app is interrupted, what this many millis for all the threads to close
    // before shutting the application down
    private static final int THREAD_SHUTDOWN_WAIT = 10 * 1000;
    
    public static void main(String[] args) {   
        RssSuckerApp app = new RssSuckerApp();
        app.run();
    }    
    
    private void run() {
        try {
            initialize();
            mainLoop();
        } 
        finally {
            cleanup();
        }
        
    }

    private void initialize() {
        boolean result;
        result = readProperties(); if (result == false) shutdown();
        result = processMediaDef(); if (result == false) shutdown();
        result = initEntityManagerFactory(); if (result == false) shutdown();
        result = initFilter(); if (result == false) shutdown();
        initMessaging(); 
        loadFeeds(); 
        if (feeds == null || feeds.isEmpty()) {
            logger.info("failed feed initialization or no feeds");
            shutdown();
        }
    }
    
    private boolean initFilter() {
        try {
            filter = Filter.getFilter();
            return true;
        }
        catch (Exception e) { 
            logger.logErr("filter creation failed", e);
            return false;
        }
    }
    
    private void cleanup() {
        try { 
            if (emf != null) emf.close(); 
            if (filter != null) Filter.closeFilter();
        }
        catch (Exception e) { logger.logErr("failed to cleanup", e); }
    }
           
    private void initMessaging() {
        initializeMessageQueue();
        shutdownMonitor = new ShutdownMonitor(this);
        (new Thread(shutdownMonitor)).start();
    }
    
    private void initializeMessageQueue() {
        messageQueue = new ArrayDeque<>();
    }
    
    public synchronized void sendMessage(String msg) { messageQueue.add(msg); }    
    // returns next message from the message queue, or null if there are no messages
    private synchronized String readMessage() { return messageQueue.poll(); }
        
    // read messages in the queue and do appropriate actions
    private synchronized void processMessages() 
            throws ShutdownException, FinishAndShutdownException {
        String msg;
        while ((msg = readMessage()) != null) {
            if (msg.equals(Messages.SHUTDOWN_NOW)) throw new ShutdownException();
            if (msg.equals(Messages.FINISH_AND_SHUTDOWN)) throw new FinishAndShutdownException();
        }
    }
    
    private boolean readProperties() {
        try {
            properties = new PropertiesReader(RssConfig.propertiesFile);
            numThreads = readNumericProperty("num_threads", DEFAULT_NUM_THREADS);    
            numNpapers = readNumericProperty("num_npapers", DEFAULT_NUM_NPAPERS);    
            refreshInterval = readNumericProperty("refresh_interval", DEFAULT_REFRESH_INT);                      
            // set sleepPeriod to 1/100 th of refresh interval (in miliseconds)
            sleepPeriod = refreshInterval * 60 * 1000 / 100;
            return true;
        } catch (IOException ex) { 
            logger.logErr("failed to read properties.", ex);
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
            logger.logErr("failed to parse mediadef_file", e);            
            return false;
        }
        try { // persist entities from mediadef file
            new MediadefPersister().persist(entities);        
        }
        catch (MediadefException e) {
            logger.logErr("failed to persist mediadef_file", e);            
            return false;
        }
        return true;
    }    
    
    private boolean initEntityManagerFactory() {
        try {
            emf = Factory.createEmf();
        }
        catch (Exception e) {
            logger.logErr("failed to create EntityManagerFactory", e);            
            return false;            
        }
        logger.info("initialized EntityManagerFactory");
        return true;
    }  
   
    private void mainLoop() { try {
        boolean sleep = false;
        while (true) {
            if (sleep) {
                try { Thread.sleep(MAIN_LOOP_SLEEP); } 
                catch (InterruptedException ex) {
                   logger.logInfo("main thread sleep interrupted.", ex);
                }
                sleep = false;
                processMessages();
            }            
            else { 
                processMessages();
                // decide weather to do refresh
                boolean refresh = false; 
                if (lastFeedRefresh == null) refresh = true;
                else refresh = refreshIntervalPassed();                               
                // refresh and update last refresh time
                if (refresh) {
                    doFeedRefresh();
                    lastFeedRefresh = new Date();
                }                
                sleep = true;
                processMessages();
            }
        }
    } catch (ShutdownException | FinishAndShutdownException ex) { shutdown(); }
    }

    // return true if time longer than refresh interval passed since last refresh
    private boolean refreshIntervalPassed() {
        Date now = new Date();
        double diffMin = getDifferenceInMinutes(lastFeedRefresh, now);
        if (diffMin > refreshInterval) return true;
        else return false;
    }
    
    // cleanup, releas resources and shutdown
    private void shutdown() {
        logger.logInfo("shutting down rsssucker", null);
        cleanup();
        System.exit(0);
    }
    
    // initialize threads and feed processing jobs, run them and wait for them to finish
    private void doFeedRefresh() throws ShutdownException { 
        ExecutorService executor = null;
        boolean shutdown = false;
        try {            
            
        logger.info("starting feed refresh");
        int localNumThreads = numThreads;           
        if (feeds.size() < localNumThreads) localNumThreads = feeds.size();
        executor = Executors.newFixedThreadPool(localNumThreads);        
        for (Feed f : feeds) {
            try { processMessages(); } catch (FinishAndShutdownException e) { shutdown = true; }
            IArticleScraper scraper = getNewspaper(); if (scraper == null) continue;
            IFeedReader reader = getFeedReader();            
            FeedProcessor processor = new FeedProcessor(f, emf, reader, scraper, filter);
            logger.info("starting processor for feed " + f.getUrl());
            executor.submit(processor);
        }
        executor.shutdown();
        logger.info("waiting for FeedProcessor threads to finish");
        while (executor.isTerminated() == false) {
            try { processMessages(); } catch (FinishAndShutdownException e) { shutdown = true; }
            try { executor.awaitTermination(FEED_REFRESH_SLEEP, TimeUnit.MILLISECONDS); }
            catch (InterruptedException e) {}           
        }
        logger.info("FeedProcessor threads finished");
        persistFilter();
        removeExpiredFilterEntries();
        closeNewspapers();        
        logger.info("ending feed refresh");        
        
        }
        catch (ShutdownException e) {
            // this relies on the assumption that executor will interrupt threads
            // if it does something else, use Futures instead            
            executor.shutdownNow();
            try { Thread.sleep(THREAD_SHUTDOWN_WAIT); } 
            catch (InterruptedException ex) { logger.logErr("shutdown wait interrupted", ex); }           
            closeNewspapers();
            shutdown = true;
        }        
        
        if (shutdown) throw new ShutdownException();
    }
    
    private void removeExpiredFilterEntries() {
        try { filter.removeExpiredEntries(); }
        catch (Exception e) {
            logger.logErr("removing expired entries from filter failed", e);
            resetFilter();
        }
    }

    // create new Filter class (used in case of fiter error)
    private void resetFilter() {
        try {
            Filter.resetFilter();
            filter = Filter.getFilter();
        } catch (Exception ex) { 
            logger.logErr("filter reset failed", ex);
            filter = null;
        }        
    }
    
    private void persistFilter() {
        try { filter.persistNewEntries(); }
        catch (Exception e) {
            logger.logErr("filter persisting failed", e);
            resetFilter();
        }
    }    
    
    // read all feeds from the database
    private void loadFeeds() {  
        EntityManager em = null;
        try {
            em = emf.createEntityManager();
            Query q = em.createNamedQuery("Feed.getAll");
            feeds = (List<Feed>)q.getResultList();            
        }
        catch (Exception e) {
            logger.logErr("feed refresh failed", e);
            feeds = null;
        }
        finally { if (em != null) em.close(); }
    }
    
    // read integer property from properties file
    private int readNumericProperty(String propName, int defaultValue) {
        int nt;
        try { nt = Integer.parseInt(properties.getProperty(propName)); }
        catch (NumberFormatException|NullPointerException e) { nt = defaultValue; }
        if (nt <= 0) nt = defaultValue;
        return nt;
    }
    
    // create newspaper instance and cache for later shutdown
    // if max. number of newspapers is exceeded, return existing instances
    // in round robin fashion
    private RessurectingNewspaper getNewspaper() {
        if (npapers == null) { 
            npapers = new ArrayList<RessurectingNewspaper>();
            npaperPointer = 0;
        }
        
        if (npapers.size() < numNpapers) {
            RessurectingNewspaper npaper = createNewspaper();
            if (npaper == null) return null;
            else { 
                npapers.add(npaper);
                return npaper;
            }
        }
        else {
            RessurectingNewspaper npaper = npapers.get(npaperPointer++);
            if (npaperPointer == npapers.size()) npaperPointer = 0;
            return npaper;
        }
    }
    
    private void closeNewspapers() {
        for (RessurectingNewspaper npaper : npapers) {
            try { npaper.close(); }
            catch (Exception ex) { logger.logErr("closing newspaper error", ex); }
        }      
        npapers = null;
    }
    
    // initialize and return a new newspaper instance
    private RessurectingNewspaper createNewspaper() {
        try {
            return new RessurectingNewspaper();
        } catch (IOException ex) {
            logger.logErr("failed to initialize Newspaper.", ex);
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
