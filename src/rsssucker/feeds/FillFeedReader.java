package rsssucker.feeds;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
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
        // setup base uri from feed url
        Document doc = Jsoup.connect(feedUrl).get();     
        URI baseUri = null;
        try {
            baseUri = new URI(doc.baseUri());
        } catch (URISyntaxException ex) {
            logger.logErr("failed parsing URI: "+doc.baseUri(), ex);            
        }
        boolean doResolve = true;
        int numLinks = 0;
        while ((line = reader.readLine()) != null) {
            line = line.trim();
            if ("".equals(line)) continue; // skip blank lines
            if (line.startsWith(dateMarker)) {
                date = parseDate(line.substring(dateMarker.length()));
                System.out.println(date);
                continue;
            }
            FeedEntry entry = new FeedEntry();                         
            String entryUrl;
            // compute url from line
            if (doResolve) {
                if (baseUri == null) entryUrl = line;
                else try {
                    entryUrl = resolveToBase(baseUri, line);
                } catch (URISyntaxException ex) {
                    logger.logErr("failed resolving URI for base|uri: "+baseUri+"|"+line, ex);
                    entryUrl = line;
                }
            }
            else { entryUrl = line; }      
            System.out.println(entryUrl);
            entry.setUrl(entryUrl);
            entry.setRedirUrl(entryUrl);
            entry.setDate(date);            
            entries.add(entry);
            numLinks++;
        }        
        reader.close();
        System.out.println("NUMBER OF LINKS: "+numLinks);
        return entries;
    }
    
    private String resolveToBase(URI baseUri, String line) throws URISyntaxException {
        URI uri = new URI(line);            
        URI res = baseUri.resolve(uri);       
        return res.toString();        
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
