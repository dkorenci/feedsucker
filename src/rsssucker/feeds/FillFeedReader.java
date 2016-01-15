package rsssucker.feeds;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads "fill feed" file with data for filling an existing DB Feed
 * with articles. The file contains urls and other information 
 * neccesary to construct FeedEntry objects with data.  
 * It does not getFeedEntries from feedUrl, but from file it was
 * initialized with, due to current operaion of FeedProcessor. 
 */
public class FillFeedReader implements IFeedReader {

    private String fillFile;
    
    public FillFeedReader(String file) {
        fillFile = file;
    }
    
    @Override
    public List<FeedEntry> getFeedEntries(String feedUrl) throws IOException  {
        BufferedReader reader = new BufferedReader(new FileReader(fillFile));
        String line;        
        List<FeedEntry> entries = new ArrayList<FeedEntry>();
        while ((line = reader.readLine()) != null) {
            line = line.trim();
            if ("".equals(line)) continue; // skip blank lines
            FeedEntry entry = new FeedEntry();
            entry.setUrl(line);
            entry.setRedirUrl(line);
            entries.add(entry);
        }        
        reader.close();
        return entries;
    }
    
}
