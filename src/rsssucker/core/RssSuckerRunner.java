package rsssucker.core;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import rsssucker.RssSucker;
import rsssucker.article.newspaper.Newspaper;
import rsssucker.article.newspaper.NewspaperException;
import rsssucker.article.newspaper.NewspaperOutput;
import rsssucker.config.PropertiesReader;
import rsssucker.config.RssConfig;
import rsssucker.data.Factory;
import rsssucker.data.JpaContext;
import rsssucker.data.NewsArticle;
import rsssucker.feeds.FeedEntry;
import rsssucker.feeds.FeedReader;
import rsssucker.log.LoggersManager;
import rsssucker.util.Timer;

/**
 * Entry point for RssSucker application. 
 * Initializes and runs the application.
 */
public class RssSuckerRunner {

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
    
    public RssSuckerRunner() {
        
    }
    
    public static void main(String[] args) {   
        RssSuckerRunner runner = new RssSuckerRunner();
        runner.run();
    }
    
    public void run() {
        initialize();
        mainLoop();
    }

    private void initialize() {
        try {
            properties = new PropertiesReader(RssConfig.propertiesFile);
            refreshInterval = Integer.parseInt(properties.getProperty("refresh_interval"));
            // set sleepPeriod to 1/100 th of refresh interval (in miliseconds)
            sleepPeriod = refreshInterval * 60 * 1000 / 100;
        } catch (IOException ex) {
            errLogger.log(Level.SEVERE, 
                    "failed to read properties. terminating.", ex); 
            terminate = true;
        } catch (NumberFormatException ex) {
            errLogger.log(Level.SEVERE, 
                    "failed to parse refresh_interval value. terminating.", ex);             
            terminate = true;
        }       
        feedReader = new FeedReader();
        try {
            feedReader.readFeedsFromFile();
        } catch (IOException ex) {
            errLogger.log(Level.SEVERE, 
                    "failed to read list of feeds. terminating.", ex);  
            terminate = true;            
        }
        try {
            newspaper = new Newspaper();
        } catch (IOException ex) {
            errLogger.log(Level.SEVERE, "failed to initialize Newspaper. terminating.", ex);
            terminate = true;
        }
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
                // set last refresh to now
                lastFeedRefresh = new Date();
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
        infoLogger.log(Level.INFO, "number of new entries: " + entries.size());
        for (FeedEntry e : entries) {
            NewspaperOutput news;
            try { 
                Timer t = new Timer();
                news = newspaper.processUrl(e.getArticleURL());
                infoLogger.log(Level.INFO, "time to newspaper: " + 
                        t.fromStart() + " for article: " + e.getArticleURL());
            } catch (Exception ex) {
                errLogger.log(Level.SEVERE, "failed to process URL: " + e.getArticleURL(), ex);
                continue;
            }
            saveArticle(e, news);
        }
        infoLogger.log(Level.INFO, "ending feed refresh, it lasted " + timer.fromStart());
    }

    // persist article to database
    private void saveArticle(FeedEntry feedEntry, NewspaperOutput news) {
        // copy data
        NewsArticle article = new NewsArticle();        
        article.setDatePublished(feedEntry.getDate());
        article.setExtractedTitle(news.getTitle());
        article.setFeedTitle(feedEntry.getTitle());
        article.setDescription(feedEntry.getDescription());
        article.setText(news.getText());
        article.setUrl(feedEntry.getArticleURL());
        try {
            // open transaction context
            JpaContext ctx = Factory.createContext();        
            // save, commit, close
            ctx.em.persist(article);        
            ctx.close();
            infoLogger.log(Level.INFO, "article successfuly saved: " + article.getUrl());
        } catch (Exception e) {
            errLogger.log(Level.SEVERE, "saving article failed " + article.getUrl(), e);
        }        
    }    
    
    // return absolute value difference of two time points, in minutes
    private double getDifferenceInMinutes(Date date1, Date date2) {
        final int milisPerMinute = 60*1000;
        double diff = Math.abs(date2.getTime() - date1.getTime());
        return diff/milisPerMinute;
    }
    
}
