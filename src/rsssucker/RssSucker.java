/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package rsssucker;

import com.sun.syndication.feed.synd.SyndCategory;
import com.sun.syndication.feed.synd.SyndContent;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.io.SyndFeedInput;
import com.sun.syndication.io.XmlReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.html.HtmlParser;
import org.apache.tika.sax.BodyContentHandler;
import org.apache.tika.sax.LinkContentHandler;
import org.apache.tika.sax.TeeContentHandler;
import org.apache.tika.sax.ToHTMLContentHandler;
import org.xml.sax.ContentHandler;
import rsssucker.article.newspaper.Newspaper;
import rsssucker.article.newspaper.NewspaperException;
import rsssucker.article.newspaper.NewspaperOutput;
import rsssucker.config.PropertiesReader;
import rsssucker.config.RssConfig;
import rsssucker.data.mediadef.MediadefParser;
import rsssucker.log.LoggersManager;

/**
 *
 * @author dam1root
 */
public class RssSucker {
   
    private static String guardian1="http://www.theguardian.com/world/europe/roundup/rss";
    private static String cnn1="http://rss.cnn.com/rss/edition.rss";
    private static String article1="http://www.theguardian.com/environment/2014/jul/28/bee-research-funding-pesticides-mps";
    private static String article2="http://edition.cnn.com/2014/07/27/world/meast/mideast-crisis-reporters-notebook/index.html?eref=edition";

    private static final Logger logger = LoggersManager.getErrorLogger(RssSucker.class.getName());
    
    public static void main(String[] args) throws Exception {        
        //DataOperations.test();
        //testNewspaper();
        //testLogging();
        testMediadef();
    }
    
    public static void testMediadef() throws Exception {
//        Matcher m = Pattern.compile("\\p{Alpha}+").matcher("abc def");
//        m.lookingAt(); System.out.println(m.start()+" "+m.end());
        PropertiesReader properties = new PropertiesReader(RssConfig.propertiesFile);
        String mediadefFile = properties.getProperty("mediadef_file");
        MediadefParser parser = new MediadefParser(mediadefFile);
        parser.parse();
    }
    
    public static void testNewspaper() throws IOException, NewspaperException {
        Newspaper newspaper = new Newspaper();
        NewspaperOutput out = newspaper.processUrl(article1);
        System.out.println("title: " + out.getTitle());
        System.out.println(out.getText());
        out = newspaper.processUrl(article2);
        System.out.println("title: " + out.getTitle());
        System.out.println(out.getText());        
    }
    
    public static void testLogging() {
        try {
            throw new RuntimeException("this is a runtime exception");
        }
        catch (Exception e) {
            logger.log(Level.SEVERE, "error1", e);            
        }
    }
    
    public void readAndPrintFeed(String feedURL) throws Exception {
        SyndFeedInput input = new SyndFeedInput();                
        SyndFeed feed = input.build(new XmlReader(new URL(feedURL)));
        System.out.println("FEED: " + feed.getDescription());
        System.out.println("type: "+feed.getFeedType()+"\n"+
                "copyright: "+feed.getCopyright()+"\n"+
                "author: "+feed.getAuthor()+"\n"+
                "uri: "+feed.getUri()+"\n"+
                "language: "+feed.getLanguage()+"\n"+
                "link: "+feed.getLink()+"\n");
        //System.out.println(feed);
        List entries = feed.getEntries();
        System.out.println("ENTRIES: ");
        for (Object o : entries) {
            SyndEntry e = (SyndEntry)o;
            System.out.println("FEED ENTRY:");
            System.out.println("title: "+e.getTitle()+"\n"+
                    "date published: "+e.getPublishedDate()+"\n"+
                    "author: "+e.getAuthor()+"\n"+                    
                    "link: "+e.getLink()+"\n"+
                    "uri: "+e.getUri());      
            System.out.println("- categories: ");
            List cat = e.getCategories();
            for (Object oc : cat) {
                SyndCategory c = (SyndCategory)oc;
                System.out.println("name: "+c.getName()+"\n"+
                        "taxonomy uri: "+c.getTaxonomyUri());
            }            
            
            SyndContent content = e.getDescription();
            System.out.println("content: "+content.getValue());       
            System.out.println("--- entry end ---");
//          List contents = e.getContents();            
//            System.out.println("- contents: ");
//            for (Object oc : contents) {
//                SyndContent c = (SyndContent)oc;
//                System.out.println("type: "+c.getType());
//                System.out.println("value:");
//                System.out.println(c.getValue());
//            }
        }
    }

    public void tikaExtractURL(String urlString) throws Exception {
         URL url = new URL(urlString);
         InputStream input = url.openStream();
         LinkContentHandler linkHandler = new LinkContentHandler();
         ContentHandler textHandler = new BodyContentHandler();
         ToHTMLContentHandler toHTMLHandler = new ToHTMLContentHandler();
         TeeContentHandler teeHandler = new TeeContentHandler(linkHandler, textHandler, toHTMLHandler);
         Metadata metadata = new Metadata();
         ParseContext parseContext = new ParseContext();
         HtmlParser parser = new HtmlParser();
         parser.parse(input, teeHandler, metadata, parseContext);
         System.out.println("title:\n" + metadata.get("title"));
         //System.out.println("links:\n" + linkHandler.getLinks());
         System.out.println("text:\n" + textHandler.toString());
         //System.out.println("html:\n" + toHTMLHandler.toString());
     }     
    
}
