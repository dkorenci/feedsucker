package rsssucker.feeds;

import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.io.FeedException;
import com.sun.syndication.io.SyndFeedInput;
import com.sun.syndication.io.XmlReader;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import rsssucker.data.entity.Feed;

/**
 * Feed Reader using rome framework 
 */
public class RomeFeedReader implements IFeedReader {

    @Override
    public List<FeedEntry> getFeedEntries(String feedUrl)        
            throws FeedException, IOException {
        SyndFeedInput input = new SyndFeedInput();                
        SyndFeed feed = input.build(new XmlReader(new URL(feedUrl))); 
        List entries = feed.getEntries();
        List<FeedEntry> result = new ArrayList<>(entries.size());
        for (Object o : entries) {
            SyndEntry e = (SyndEntry)o;
            e.setLink(e.getLink().trim());
            result.add(syndEntryToFeedEntry(e));
        }
        return result;
    } 
    
    private static FeedEntry syndEntryToFeedEntry(SyndEntry e) {
        FeedEntry entry = new FeedEntry();
        entry.setArticleURL(e.getLink());
        entry.setDate(e.getPublishedDate());
        entry.setTitle(e.getTitle());
        entry.setDescription(e.getDescription().getValue());
        entry.setAuthor(e.getAuthor());
        return entry;
    }
    
    // parse feed url, read data and write it to entity file
    public static void readFeedData(Feed efeed) throws FeedException, IOException {
        SyndFeedInput input = new SyndFeedInput();                
        SyndFeed feed = input.build(new XmlReader(new URL(efeed.getUrl())));         
        efeed.setTitle(feed.getTitle());
        efeed.setDescription(feed.getDescription());        
    }
    
}
