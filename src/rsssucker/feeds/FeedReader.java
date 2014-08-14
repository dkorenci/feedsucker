/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package rsssucker.feeds;

import rsssucker.data.Factory;
import rsssucker.data.JpaContext;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.io.FeedException;
import com.sun.syndication.io.SyndFeedInput;
import com.sun.syndication.io.XmlReader;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.persistence.Query;
import rsssucker.config.PropertiesReader;
import rsssucker.config.RssConfig;
import rsssucker.data.NewsArticle;

/**
 *
 */
public class FeedReader {

    private PropertiesReader properties;    
    private Set<String> feeds;

    /** Read RSS feeds from a file specified in properties, 
     * one feed URL per line, add new feeds to the list.
     * Can be called repeatedly if feeds are added to the file. */
    public void readFeedsFromFile() throws IOException {
        if (properties == null) properties = new PropertiesReader(RssConfig.propertiesFile);      
        if (feeds == null) feeds = new TreeSet<String>();
        BufferedReader reader = new BufferedReader(
                new FileReader(properties.getProperty("feedlist")));
        String line;        
        while ((line = reader.readLine()) != null) {
            feeds.add(line.trim());            
        }        
    }
    
    /** Get new article URLs from all the feeds in the list. */
    public List<FeedEntry> getNewArticles() {        
        List<SyndEntry> entries = getAllFeedEntries();
        // get URLs urls of already saved articles that could be in the feed
        // i.e. saved articles with date older than oldest feed date
        Date oldest = getOldestDate(entries);        
        Set<String> urlsAfterOldest = new TreeSet<String>(getArticlesAfterDate(oldest));
        // get new entries, ie those entries that are not in urlsAfterOldest
        List<FeedEntry> result = new ArrayList<FeedEntry>();
        for (SyndEntry e : entries) {
            String url = e.getLink();            
            if (urlsAfterOldest.contains(url) == false) {
                FeedEntry entry = new FeedEntry();
                entry.setArticleURL(url);
                entry.setDate(e.getPublishedDate());
                entry.setTitle(e.getTitle());
                entry.setDescription(e.getDescription().getValue());
                //e.get
                result.add(entry);
            }
        }
        
        return result;
    }
        
    // get all entries from all the feeds
    private List<SyndEntry> getAllFeedEntries() {
        List<SyndEntry> result = new ArrayList<SyndEntry>(feeds.size()*10);
        for (String feed : feeds) {
            try {
                result.addAll(getFeedEntries(feed));
            } catch (FeedException ex) {
                Logger.getLogger(FeedReader.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IOException ex) {
                Logger.getLogger(FeedReader.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return result;
    }
    
    /** Get URLs of articles in the database with published date
     * after or equal to the specified date. */
    private static List<String> getArticlesAfterDate(Date date) {
        JpaContext jpac = Factory.createContext();        
        Query q = jpac.em.createNamedQuery("getArticlesAfterDate");
        q.setParameter("date", date);
        jpac.close();
        List articles = q.getResultList();                       
        List<String> result = new ArrayList<String>();
        for (Object o : articles) {
            NewsArticle art = (NewsArticle)o;
            result.add(art.getUrl());
        }
        return result;
    }
    
    /** Get all entries in the feed. */
    private static List<SyndEntry> getFeedEntries(String feedURL) 
            throws FeedException, IOException {
        SyndFeedInput input = new SyndFeedInput();                
        SyndFeed feed = input.build(new XmlReader(new URL(feedURL))); 
        List entries = feed.getEntries();
        List<SyndEntry> result = new ArrayList<SyndEntry>();
        for (Object o : entries) {
            SyndEntry e = (SyndEntry)o;
            e.setLink(e.getLink().trim());
            result.add(e);
        }
        return result;
    }    
      
    // get oldest published date among entries
    private static Date getOldestDate(List<SyndEntry> entries) {      
        Date oldest = null;
        for (SyndEntry e : entries) {
            Date pubDate = e.getPublishedDate();
            if (oldest == null || oldest.compareTo(pubDate) > 0) oldest = pubDate;
        }
        return oldest;
    }
    
}
