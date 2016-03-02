/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package rsssucker;

import com.google.common.escape.Escaper;
import com.google.common.escape.Escapers;
import com.sun.syndication.feed.synd.SyndCategory;
import com.sun.syndication.feed.synd.SyndContent;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.io.SyndFeedInput;
import com.sun.syndication.io.XmlReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.xml.sax.ContentHandler;
import rsssucker.article.ArticleData;
import rsssucker.article.newspaper.Newspaper;
import rsssucker.article.newspaper.NewspaperException;
import rsssucker.article.newspaper.NewspaperTester;
import rsssucker.util.PropertiesReader;
import rsssucker.config.RssConfig;
import rsssucker.data.mediadef.MediadefEntity;
import rsssucker.data.mediadef.MediadefParser;
import rsssucker.data.mediadef.MediadefPersister;
import rsssucker.log.LoggersManager;
import rsssucker.tools.DatabaseTools;
import rsssucker.util.HttpUtils;

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
        //testUrlRedirect();
        //testGoogleNewsUrlParsing();
        //testEscaping();
        new DatabaseTools().exportDatabaseAsTable();
    }


    private static void testEscaping() {
        char delimit = '\t';
        String text = "who can\tescape the tab\tmonster";
        Escaper e = Escapers.builder().addEscape(delimit, "\\"+delimit).build();
        System.out.println(text);
        System.out.println(e.escape(text));
    }    
    
    private static void testGoogleNewsUrlParsing() throws Exception {
        //String url = "http://news.google.com/news/url?sr=1&ct2=us%2F0_0_s_0_12_a&sa=t&usg=AFQjCNFcJYqcno20DdIVUEAwgeSb19w6_g&cid=52778672338616&url=http%3A%2F%2Fwww.msnbc.com%2Fmsnbc%2Fmike-browns-stepdad-apologizes-outburst&ei=hDB_VIC-FYjA1Aa1xoGABQ&rt=HOMEPAGE&vm=STANDARD&bvm=section&did=-2511018995557392677&ssid=h&gcnid=840";        
        String url = "http://news.google.com/news/url?sr=1&ct2=us%2F0_0_s_1_1_a&sa=t&usg=AFQjCNGhpS94Oggrx4lZ2dY08MudICcm_A&cid=52778672704092&url=http%3A%2F%2Fabcnews.go.com%2FTechnology%2Fnasas-orion-spacecraft-suffers-series-setbacks-launch-day%2Fstory%3Fid%3D27360581&ei=AnOAVJCqDMLR1QbOzIDICQ&rt=HOMEPAGE&vm=STANDARD&bvm=section&did=-3744730547277117519&ssid=h";
        URI uri = new URI(url);        
        System.out.println(HttpUtils.resolveGoogleRedirect(url));
    }
    
    private static void testUrlRedirect() throws IOException, URISyntaxException {
        CloseableHttpClient httpclient = HttpClients.createDefault();
        HttpClientContext context = HttpClientContext.create();        
        //String uri="http://feeds.nbcnews.com/c/35002/f/663303/s/410be3e0/sc/1/l/0L0Snbcnews0N0Cpolitics0Cfirst0Eread0Csimple0Ecommon0Esense0Ejeh0Ejohnson0Edefends0Eexecutive0Eaction0Eimmigration0En259736/story01.htm";
        //String uri="http://feeds.theguardian.com/c/34708/f/663879/s/410c0702/sc/8/l/0L0Stheguardian0N0Cworld0C20A140Cdec0C0A20Cnorth0Ekorea0Esony0Ecyber0Eattack/story01.htm";
        //String uri = "http://feeds.reuters.com/~r/Reuters/worldNews/~3/ySVJ_LFYBrs/story01.htm";
        String uri = "http://rss.nytimes.com/c/34625/f/642565/s/40f27c2a/sc/20/l/0L0Snytimes0N0C20A140C110C290Cworld0Cmiddleeast0Cpalestinian0Ehaven0Efor0E60Edecades0Enow0Eflooded0Efrom0Esyria0E0Bhtml0Dpartner0Frss0Gemc0Frss/story01.htm";
        HttpGet httpget = new HttpGet(uri);
        httpget.addHeader("User-Agent", "Mozilla/5.0 (X11; Linux x86_64; rv:17.0) Gecko/20121202 Firefox/17.0 Iceweasel/17.0.1");        
        CloseableHttpResponse response = httpclient.execute(httpget, context);          
        try {
            HttpHost target = context.getTargetHost();
            System.out.println("httpget URI: " + httpget.getURI());
            System.out.println("target host: " + target);
            System.out.println(response.getStatusLine().getStatusCode());            
            for (Header h : response.getAllHeaders()) {
                System.out.println(h.getName()+ " : " + h.getValue());                
            }
//            BufferedReader br = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
//            String line = null;
//            while ((line = br.readLine()) != null) {
//                System.out.println(line);
//            }
            List<URI> redirectLocations = context.getRedirectLocations();
            if (redirectLocations != null)
            for (URI loc : redirectLocations) {
                System.out.println("redirect location: " + loc);
            }
            URI location = URIUtils.resolve(httpget.getURI(), target, redirectLocations);
            System.out.println("Final HTTP location: " + location.toASCIIString());
            // Expected to be an absolute URI
        } finally {
            response.close();
        }
    }    
    
    public static void testNewspaper2() throws Exception {
        (new NewspaperTester()).concurrencyTest(10, 500);
    }
    
    public static void testMediadef() throws Exception {
//        Matcher m = Pattern.compile("\\p{Alpha}+").matcher("abc def");
//        m.lookingAt(); System.out.println(m.start()+" "+m.end());
        PropertiesReader properties = new PropertiesReader(RssConfig.propertiesFile);
        String mediadefFile = properties.getProperty("mediadef_file");
        MediadefParser parser = new MediadefParser(mediadefFile);
        List<MediadefEntity> entities = parser.parse();
        new MediadefPersister(null).persist(entities);
    }
    
    public static void testNewspaper() throws IOException, NewspaperException {
        Newspaper newspaper = new Newspaper("en");
        ArticleData out = newspaper.scrapeArticle(article1);
        System.out.println("title: " + out.getTitle());
        System.out.println(out.getText());
        out = newspaper.scrapeArticle(article2);
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
    
}
