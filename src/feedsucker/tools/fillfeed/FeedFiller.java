package feedsucker.tools.fillfeed;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import feedsucker.util.PropertiesReader;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Query;
import feedsucker.scrape.IArticleScraper;
import feedsucker.config.FeedsuckerConfig;
import feedsucker.core.FeedsuckerApp;
import feedsucker.core.feedprocessor.FeedProcessor;
import feedsucker.data.Factory;
import feedsucker.data.entity.Feed;
import feedsucker.feedreader.file.FileFeedReader;
import feedsucker.feedreader.IFeedReader;
import feedsucker.log.FeedsuckerLogger;

/**
 * Read read article URLs from files and insert them into database
 * as if they came from an existing feed specified in the file. 
 * Used for later insertion of URLs that were missed due to 
 * application downtime. 
 */
public class FeedFiller {

    private EntityManagerFactory emf;
    private int feedReadPause, articleReadPause;
    
    private static final FeedsuckerLogger logger = 
            new FeedsuckerLogger(FeedsuckerApp.class.getName());    
    
    private boolean readProperties() {
        try {
            PropertiesReader properties = new PropertiesReader(FeedsuckerConfig.propertiesFile); 
            //userAgent = properties.getProperty("user_agent");
            //numThreads = properties.readIntProperty("num_threads", DEFAULT_NUM_THREADS);    
            //numNpapers = properties.readIntProperty("num_npapers", DEFAULT_NUM_NPAPERS);    
            //refreshInterval = properties.readIntProperty("refresh_interval", DEFAULT_REFRESH_INT); 
            feedReadPause = properties.readIntProperty("feed_read_pause", 
                                        FeedsuckerApp.DEFAULT_FEED_PAUSE); 
            articleReadPause = properties.readIntProperty("article_read_pause", 
                                            FeedsuckerApp.DEFAULT_ARTICLE_PAUSE); 
            // set sleepPeriod to 1/100 th of refresh interval (in miliseconds)
            //sleepPeriod = refreshInterval * 60 * 1000 / 100;
            return true;
        } catch (IOException ex) { 
            logger.logErr("failed to read properties.", ex);
            return false; 
        } 
    }    
    
    /**
     * Setup and run feed filling
     * @param arg - folder with fill files or a fill file
     */
    public void run(String arg[]) {
        try {
            File file = new File(arg[0]);
            if (!file.exists()) {
                String msg = "file "+arg[0]+" does not exist";
                logger.logErr(msg, null);
                System.out.println(msg);
                return;
            }
            // if file is directory, process all containing files, else process only the file
            List<File> files = new ArrayList<>();
            if (file.isDirectory()) files = Arrays.asList(file.listFiles());
            else files.add(file);
            emf = Factory.createEmf();
            for (File f : files) processFile(f);        
        }
        catch (IOException ex) { logger.logErr("", ex); }
        finally { if (emf != null) emf.close(); }
    }
    
    /**
     * Read article urls from file and save them as entries of the
     * feed already in the database, whose url must be specified on
     * the first line of the file.
     */
    private void processFile(File file) throws IOException {                
        BufferedReader reader = new BufferedReader(new FileReader(file));
        String feedUrl;       
        // first line must be url of a feed from the database
        feedUrl = reader.readLine().trim(); reader.close();
        Feed f = fetchFeed(feedUrl);
        if (f == null) throw new 
            IllegalArgumentException("feed "+feedUrl+" is not in the database");
        IArticleScraper scraper = FeedsuckerApp.createNewspaper(f.getLanguage());        
        IFeedReader feedReader = new FileFeedReader(file.getAbsolutePath());
        FeedProcessor fproc = 
                new FeedProcessor(f, emf, feedReader, scraper, null, 
                        feedReadPause, articleReadPause);       
        fproc.run();
    }
    
    // attempt to load feed from database by url
    private Feed fetchFeed(String feedUrl) {
        EntityManager em = null;
        try {
            em = emf.createEntityManager();    
            Query q = em.createNamedQuery("Feed.getByUrl");
            q.setParameter("url", feedUrl);               
            try { 
                Object res = q.getSingleResult(); 
                return (Feed)res;
            }
            catch (Exception ex) { return null; }                        
        }
        finally { if (em != null) em.close(); }
    }
    
}
