/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package rsssucker;

import com.sun.syndication.feed.synd.SyndCategory;
import com.sun.syndication.feed.synd.SyndContent;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndEntryImpl;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.io.SyndFeedInput;
import com.sun.syndication.io.XmlReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.html.HtmlParser;
import org.apache.tika.sax.BodyContentHandler;
import org.apache.tika.sax.LinkContentHandler;
import org.apache.tika.sax.TeeContentHandler;
import org.apache.tika.sax.ToHTMLContentHandler;
import org.python.core.PyCode;
import org.xml.sax.ContentHandler;
import org.python.core.PyException;
import org.python.core.PyInteger;
import org.python.core.PyObject;
import org.python.core.PyString;
import org.python.util.PythonInterpreter;
import rsssucker.data.DataOperations;

/**
 *
 * @author dam1root
 */
public class RssSucker {

    /**
     * @param args the command line arguments
     */
    
    private static String guardian1="http://www.theguardian.com/world/europe/roundup/rss";
    private static String cnn1="http://rss.cnn.com/rss/edition.rss";
    private static String article1="http://www.theguardian.com/environment/2014/jul/28/bee-research-funding-pesticides-mps";
    private static String article2="http://edition.cnn.com/2014/07/27/world/meast/mideast-crisis-reporters-notebook/index.html?eref=edition";
    
    public static void main(String[] args) throws Exception {
        //DataOperations.test();
        RssSucker sucker = new RssSucker();
        sucker.readAndPrintFeed(guardian1);
        //sucker.tikaExtractURL("http://www.theguardian.com/culture/2014/jul/28/tulisa-contostavlos-paranoid-wreck-fake-sheikh-mazher-mahmood-drugs-sting");
        //sucker.tikaExtractURL("http://edition.cnn.com/2014/07/27/world/meast/mideast-crisis-reporters-notebook/index.html?eref=edition");
        //sucker.tikaExtractURL("http://www.theguardian.com/environment/2014/jul/28/bee-research-funding-pesticides-mps");
        //sucker.testPython();
        //sucker.testNewspaper();
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

    public void testNewspaper() {
        PythonInterpreter interp = new PythonInterpreter();                        
        interp.exec("newspaper/extract.py");        
        PyObject someFunc = interp.get("processArticle");
        PyObject result = someFunc.__call__(new PyString(article1));
        String realResult = (String) result.__tojava__(String.class);                
        System.out.println(realResult);        
//        interpreter.exec("import sys\nsys.path.append('pathToModiles if they're not there by default')\nimport yourModule");
//        PyObject someFunc = interpreter.get("funcName");
//        PyObject result = someFunc.__call__(new PyString("Test!"));
//        String realResult = (String) result.__tojava__(String.class);                
    }
    
    public void testPython() throws PyException {

        // Create an instance of the PythonInterpreter
        PythonInterpreter interp = new PythonInterpreter();
        
        // The exec() method executes strings of code
        interp.exec("import sys");
        interp.exec("print sys");

        // Set variable values within the PythonInterpreter instance
        interp.set("a", new PyInteger(42));
        interp.exec("print a");
        interp.exec("x = 2+2");

        // Obtain the value of an object from the PythonInterpreter and store it
        // into a PyObject.
        PyObject x = interp.get("x");
        System.out.println("x: " + x);
    }    
    
}
