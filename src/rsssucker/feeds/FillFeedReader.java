package rsssucker.feeds;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import rsssucker.log.RssSuckerLogger;

/**
 * Reads "fill feed" file with data for filling an existing DB Feed
 * with articles. The file contains urls and other information 
 * neccesary to construct FeedEntry objects with data.  
 * It does not getFeedEntries from feedUrl, but from file it was
 * initialized with, due to current operaion of FeedProcessor. 
 */
public class FillFeedReader implements IFeedReader {

    private String fillFile;

    private static final RssSuckerLogger logger = 
            new RssSuckerLogger(FillFeedReader.class.getName());            
    
    public FillFeedReader(String file) {
        fillFile = file;
    }
    
    private static final String UNKNOWN_DATE="[unknown]";
    private static final String DATE_FORMAT="dd.MM.yyyy-HH:mm:ss";
    
    @Override
    public List<FeedEntry> getFeedEntries(String feedUrl) throws IOException  {
        BufferedReader reader = new BufferedReader(new FileReader(fillFile));
        String line;        
        List<FeedEntry> entries = new ArrayList<FeedEntry>();        
        Date date = null; String dateMarker = "[date]";
        reader.readLine(); // skip feed url on the first line
        while ((line = reader.readLine()) != null) {
            line = line.trim();
            if ("".equals(line)) continue; // skip blank lines
            if (line.startsWith(dateMarker)) {
                date = parseDate(line.substring(dateMarker.length()));
                System.out.println(date);
                continue;
            }
            FeedEntry entry = new FeedEntry();
            entry.setUrl(line);
            entry.setRedirUrl(line);
            entry.setDate(date);            
            entries.add(entry);
        }        
        reader.close();
        return entries;
    }
    
    public Date parseDate(String dateString) {
        dateString = dateString.trim();        
        if (UNKNOWN_DATE.equals(dateString)) return null;
        SimpleDateFormat parser = new SimpleDateFormat(DATE_FORMAT);
        try { 
            Date date = parser.parse(dateString);
            return date;
        } catch (ParseException ex) {
            logger.logErr("failed to parse date: "+dateString, ex);
            return null;
        }        
    }
    
}
