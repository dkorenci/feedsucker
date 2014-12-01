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
import rsssucker.article.newspaper.Newspaper;
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
    private List<Newspaper> npapers;
    private Queue<String> messageQueue;
    private ShutdownMonitor shutdownMonitor;
    private Filter filter;
    
    private static final Logger errLogger = 
            LoggersManager.getErrorLogger(RssSuckerApp.class.getName());
    private static final Logger infoLogger = 
            LoggersManager.getInfoLogger(RssSuckerApp.class.getName());   
    private static final int DEFAULT_NUM_THREADS = 10;
       
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
    }
    
    private boolean initFilter() {
        try {
            filter = Filter.getFilter();
            return true;
        }
        catch (Exception e) { 
            logErr("filter creation failed", e);
            return false;
        }
    }
    
    private void cleanup() {
        try { 
            if (emf != null) emf.close(); 
            if (filter != null) Filter.closeFilter();
        }
        catch (Exception e) { logErr("failed to cleanup", e); }
    }
    
    private static void logErr(String msg, Exception e) {
        errLogger.log(Level.SEVERE, msg, e);        
    }
    
    // send info message
    private static void info(String msg) { 
        String header = String.format("[%1$td%1$tm%1$tY_%1$tH:%1$tM:%1$tS] : ", new Date());
        System.out.println(header + msg); 
    }
    
    private static void logInfo(String msg, Exception e) {
        infoLogger.log(Level.INFO, msg, e);        
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
   
    private void mainLoop() { try {
        boolean sleep = false;
        while (true) {
            if (sleep) {
                try { Thread.sleep(MAIN_LOOP_SLEEP); } 
                catch (InterruptedException ex) {
                   logInfo("main thread sleep interrupted.", ex);
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
        logInfo("shutting down rsssucker", null);
        cleanup();
        System.exit(0);
    }
    
    // initialize threads and feed processing jobs, run them and wait for them to finish
    private void doFeedRefresh() throws ShutdownException { 
        ExecutorService executor = null;
        boolean shutdown = false;
        try {            
            
        info("starting feed refresh");
        List<Feed> feeds = getFeeds(); //feeds = feeds.subList(0, 1);
        int numThreads = getNumThreads();    
        if (feeds.size() < numThreads) numThreads = feeds.size();
        executor = Executors.newFixedThreadPool(numThreads);        
        for (Feed f : feeds) {
            try { processMessages(); } catch (FinishAndShutdownException e) { shutdown = true; }
            IArticleScraper scraper = getNewspaper(); if (scraper == null) continue;
            IFeedReader reader = getFeedReader();            
            FeedProcessor processor = new FeedProcessor(f, emf, reader, scraper, filter);
            info("starting processor for feed " + f.getUrl());
            executor.submit(processor);
        }
        executor.shutdown();
        info("waiting for FeedProcessor threads to finish");
        while (executor.isTerminated() == false) {
            try { processMessages(); } catch (FinishAndShutdownException e) { shutdown = true; }
            try { executor.awaitTermination(FEED_REFRESH_SLEEP, TimeUnit.MILLISECONDS); }
            catch (InterruptedException e) {}           
        }
        info("FeedProcessor threads finished");
        persistFilter();
        removeExpiredFilterEntries();
        closeNewspapers();        
        info("ending feed refresh");        
        
        }
        catch (ShutdownException e) {
            // this relies on the assumption that executor will interrupt threads
            // if it does something else, use Futures instead            
            executor.shutdownNow();
            try { Thread.sleep(THREAD_SHUTDOWN_WAIT); } 
            catch (InterruptedException ex) { logErr("shutdown wait interrupted", ex); }           
            closeNewspapers();
            shutdown = true;
        }        
        
        if (shutdown) throw new ShutdownException();
    }
    
    private void removeExpiredFilterEntries() {
        try { filter.removeExpiredEntries(); }
        catch (Exception e) {
            logErr("removing expired entries from filter failed", e);
            resetFilter();
        }
    }

    // create new Filter class (used in case of fiter error)
    private void resetFilter() {
        try {
            Filter.resetFilter();
            filter = Filter.getFilter();
        } catch (Exception ex) { 
            logErr("filter reset failed", ex);
            filter = null;
        }        
    }
    
    private void persistFilter() {
        try { filter.persistNewEntries(); }
        catch (Exception e) {
            logErr("filter persisting failed", e);
            resetFilter();
        }
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
