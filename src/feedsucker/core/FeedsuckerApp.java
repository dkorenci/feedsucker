package feedsucker.core;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Query;
import feedsucker.scrape.IArticleScraper;
import feedsucker.scrape.newspaper.ResurrectingNewspaper;
import feedsucker.util.PropertiesReader;
import feedsucker.config.FeedsuckerConfig;
import feedsucker.core.feedprocessor.FeedProcessor;
import feedsucker.core.messages.FinishAndShutdownException;
import feedsucker.core.messages.ShutdownException;
import feedsucker.core.messages.MessageFileMonitor;
import feedsucker.core.messages.MessageReceiver;
import feedsucker.data.Factory;
import feedsucker.data.filter.Filter;
import feedsucker.data.entity.Feed;
import feedsucker.data.mediadef.MediadefEntity;
import feedsucker.data.mediadef.MediadefException;
import feedsucker.data.mediadef.MediadefParser;
import feedsucker.data.mediadef.MediadefPersister;
import feedsucker.feedreader.IFeedReader;
import feedsucker.feedreader.rome.RomeFeedReader;
import feedsucker.feedreader.html.HtmlFeedReader;
import feedsucker.log.FeedsuckerLogger;
import feedsucker.resources.ResourceFactory;
import feedsucker.tools.DatabaseTools;
import feedsucker.tools.fillfeed.FeedFiller;

/**
 * Main class, implementing initialization and workflow of the application.
 */
public class FeedsuckerApp {
    
    private Date lastFeedRefresh;
    // feeds refresh interval, in minutes
    private int refreshInterval;
    // waits after the network read operations, in milis
    private int feedReadPause, articleReadPause;
    // main thread sleep period, in miliseconds
    private int sleepPeriod;
    private PropertiesReader properties;    
    private String userAgent;
    private EntityManagerFactory emf;
    private List<ResurrectingNewspaper> npapers;
    private MessageReceiver messageReceiver;
    private MessageFileMonitor messageMonitor;
    private Filter filter;
    private List<Feed> feeds;    
    
    private static final FeedsuckerLogger logger = 
            new FeedsuckerLogger(FeedsuckerApp.class.getName());
    
    private int numThreads; 
    private int numNpapers, npaperPointer;    
    private static final int DEFAULT_NUM_THREADS = 10;
    private static final int DEFAULT_NUM_NPAPERS = 10;
    private static final int DEFAULT_REFRESH_INT = 30;
    public static final int DEFAULT_FEED_PAUSE = 0;
    public static final int DEFAULT_ARTICLE_PAUSE = 0;
       
    // main loop (wait for next refresh) sleep period, in milis
    private static final int MAIN_LOOP_SLEEP = 1000;
    // how long to sleep when waiting for FeedProcessor threads to finish
    private static final int FEED_REFRESH_SLEEP = 500;
    // if app is interrupted, what this many millis for all the threads to close
    // before shutting the application down
    private static final int THREAD_SHUTDOWN_WAIT = 10 * 1000;
    
    /**
     * If run without arguments, Feedsucker is started. 
     * Else, tools starter is run with the passed arguments.      
     */
    public static void main(String[] args) {   
        if (args.length == 0) {
            FeedsuckerApp app = new FeedsuckerApp();
            app.run();
        }
        else {
            runTools(args);
        }
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

    private static void runTools(String[] args) {
        String javaBin = args[0];
        System.out.println(javaBin);
        String tool = args[1].toLowerCase();        
        System.out.println(tool);
        try {
            if ("hosts".equals(tool)) new DatabaseTools().printHosts();
            else if ("table".equals(tool)) new DatabaseTools().exportDatabaseAsTable();
            else if ("loop".equals(tool)) new LoopAppRunner(javaBin).run();
            else if ("fill".equals(tool)) {
                String[] ffargs = Arrays.copyOfRange(args, 2, args.length);
                new FeedFiller().run(ffargs);
            }
            else System.out.println("unrecognized tool command");
        }
        catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace(System.out);            
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
        if (messageMonitor != null) {
            messageMonitor.stop();
            try { Thread.sleep(50); } catch (InterruptedException ex) { }
        }
    }
           
    private void initMessaging() {
        messageReceiver = new MessageReceiver();
        messageMonitor = new MessageFileMonitor("messages.txt", messageReceiver);
        messageMonitor.start();
    }  
       
    // read messages in the queue and do appropriate actions
    private void checkMessages() 
            throws ShutdownException, FinishAndShutdownException {
        messageReceiver.checkMessages();
    }
    
    private boolean readProperties() {
        try {
            properties = new PropertiesReader(FeedsuckerConfig.propertiesFile); 
            userAgent = properties.getProperty("user_agent");
            numThreads = properties.readIntProperty("num_threads", DEFAULT_NUM_THREADS);    
            numNpapers = properties.readIntProperty("num_npapers", DEFAULT_NUM_NPAPERS);    
            refreshInterval = properties.readIntProperty("refresh_interval", DEFAULT_REFRESH_INT); 
            feedReadPause = properties.readIntProperty("feed_read_pause", DEFAULT_FEED_PAUSE); 
            articleReadPause = properties.readIntProperty("article_read_pause", DEFAULT_ARTICLE_PAUSE); 
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
            new MediadefPersister(userAgent).persist(entities);        
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
   
    /**
     * Sleep. Monitor messages passed to the application. 
     * At specified intervals run feed reading and article scraping.     
     */
    private void mainLoop() { try {
        boolean sleep = false;
        while (true) {
            if (sleep) {
                try { Thread.sleep(MAIN_LOOP_SLEEP); } 
                catch (InterruptedException ex) {
                   logger.logInfo("main thread sleep interrupted.", ex);
                }
                sleep = false;
                checkMessages();
            }            
            else { 
                checkMessages();
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
                checkMessages();
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
        logger.logInfo("shutting down feedsucker", null);
        cleanup();
        System.exit(0);
    }
    
    // initialize threads and feed processing jobs, run them and wait for them to finish
    private void doFeedRefresh() throws ShutdownException { 
        ExecutorService executor = null;
        boolean shutdown = false;
        try {            
            
        logger.info("STARTING FEED REFRESH");
        int localNumThreads = numThreads;           
        if (feeds.size() < localNumThreads) localNumThreads = feeds.size();
        executor = Executors.newFixedThreadPool(localNumThreads);        
        for (Feed f : feeds) {
            try { checkMessages(); } catch (FinishAndShutdownException e) { shutdown = true; }
            // create objects needed to process feeds
            IArticleScraper scraper; IFeedReader reader; FeedProcessor processor;
            try {
                scraper = getNewspaper(f.getLanguage()); if (scraper == null) continue;            
                reader = getFeedReader(f);            
                processor = new FeedProcessor(f, emf, reader, scraper, filter, 
                                    feedReadPause, articleReadPause);
            }
            catch(Exception e) {
                logger.logErr("error initializing processors for feed "+f.getUrl(), e);
                continue;
            }
            // submit feed processing job            
            executor.submit(processor);
        }
        executor.shutdown();        
        while (executor.isTerminated() == false) {
            try { checkMessages(); } catch (FinishAndShutdownException e) { shutdown = true; }
            try { executor.awaitTermination(FEED_REFRESH_SLEEP, TimeUnit.MILLISECONDS); }
            catch (InterruptedException e) {}           
        }        
        persistFilter();
        removeExpiredFilterEntries();
        closeNewspapers();        
        logger.info("ENDING FEED REFRESH - all feeds processed");        
        
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
    
    // create newspaper instance and cache for later shutdown
    // if max. number of newspapers is exceeded, return existing instances
    // in round robin fashion
    private ResurrectingNewspaper getNewspaper(String langCode) {
        if (npapers == null) { 
            npapers = new ArrayList<ResurrectingNewspaper>();
            npaperPointer = 0;
        }
        
        if (npapers.size() < numNpapers) {
            ResurrectingNewspaper npaper = createNewspaper(langCode);
            if (npaper == null) return null;
            else { 
                npapers.add(npaper);
                return npaper;
            }
        }
        else {
            ResurrectingNewspaper npaper = npapers.get(npaperPointer++);
            if (npaperPointer == npapers.size()) npaperPointer = 0;
            return npaper;
        }
    }
    
    private void closeNewspapers() {
        for (ResurrectingNewspaper npaper : npapers) {
            try { npaper.close(); }
            catch (Exception ex) { logger.logErr("closing newspaper error", ex); }
        }      
        npapers = null;
    }
    
    // initialize and return a new newspaper instance
    public static ResurrectingNewspaper createNewspaper(String langCode) {
        try {
            return new ResurrectingNewspaper(langCode);
        } catch (IOException ex) {
            logger.logErr("failed to initialize Newspaper.", ex);
            return null;
        }
    }
    
    private IFeedReader getFeedReader(Feed f) throws IllegalArgumentException {         
        String type = f.getType();
        // check if type is valid
        if (Feed.validTypeCode(type) == false) {
            throw new IllegalArgumentException("Invalid type code "
                    +type+" for feed"+f.getUrl());
        }
        // create feed reader depending on feed type
        if (Feed.TYPE_SYNDICATION.equals(type)) {
            if (f == null || f.getAttributes() == null) return new RomeFeedReader();
            if (f.getAttributes().toLowerCase().contains("agent")) {
                return new RomeFeedReader(userAgent); 
            }
            else
                return new RomeFeedReader();
        }
        else if (Feed.TYPE_WEBPAGE.equals(type)) {
            Set<String> words = ResourceFactory.getAsciiWordlist(f.getLanguage());        
            if (words == null) throw new IllegalArgumentException("Could not load "
                        + "language file for : "+f.getLanguage());
            else return new HtmlFeedReader(words, 4);            
        }
        else throw new IllegalArgumentException("unsuported feed type "+
                type+" for feed "+f.getUrl());
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
