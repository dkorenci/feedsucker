package feedsucker.feedreader.html;

import java.io.IOException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.net.URL;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import feedsucker.feedreader.FeedEntry;
import feedsucker.resources.ResourceFactory;

public class TestHtmlFeed {

    public static void main(String[] args) throws IOException, URISyntaxException, Exception {
        //String url = "http://www.tportal.hr/vijesti/hrvatska/";
        String url = "http://www.vijesti.rtl.hr/novosti/";
        //String url = "http://www.vecernji.hr/hrvatska/";
        Set<String> words = ResourceFactory.getAsciiWordlist("hr");        
        HtmlFeedReader reader = new HtmlFeedReader(words, 5);
        List<FeedEntry> entries = reader.getFeedEntries(url);
        System.out.println();
        System.out.println("EXTRACTED URLS: ");
        System.out.println();
        Set<String> extractedUrls = new TreeSet<String>();
        for (FeedEntry e : entries) {
            System.out.println(e.getRedirUrl());
            extractedUrls.add(e.getRedirUrl());
        }
        System.out.println();
        System.out.println("NON-EXTRACTED URLS: ");
        System.out.println();        
        for (String u : reader.getAllLinks(url))
            if (!extractedUrls.contains(u)) System.out.println(u);                           
    }
    
    // code to play with feed extraction functionality
    public static void experiment() throws IOException, URISyntaxException {
        //String url = "http://www.vecernji.hr/hrvatska/";
        String url = "http://www.tportal.hr/vijesti/hrvatska/";
        //String url = "http://www.vijesti.rtl.hr/novosti/";
        Document doc = Jsoup.connect(url).get();           
        Elements anchors = doc.select("a[href]");
        URI baseUri = new URI(doc.baseUri());
        System.out.println("base URI: "+ baseUri);
        for (Element elem : anchors) {            
            String link = elem.attr("href");            
            System.out.println(link);
            URI uri = new URI(link);            
//            System.out.println("scheme: "+uri.getScheme());
//            System.out.println("host: "+uri.getHost());
//            System.out.println("path: "+uri.getPath());
            URI res = baseUri.resolve(uri);
//            System.out.println("resolved: "+res.toString());
//            System.out.println("path: "+res.getPath());
            //System.out.println(uri.getScheme());  
            String path = res.getPath();
            if (path != null) {
                String [] split = path.split("/");
//                for (String frag: split) {
//                    System.out.println("path fragment: "+frag);
//                }
                if (split.length == 0) continue;                
                String last = split[split.length-1].toLowerCase().trim();     
//                System.out.println("last url fragment: "+last);
//                for (String frag: last.split("[^a-z]+")) {
//                    System.out.println("word fragment: "+frag);
//                }
                Pattern extPattern = Pattern.compile("^(.*)\\.([a-z]+)$");
                Matcher matcher = extPattern.matcher(last);                
                if (matcher.matches()) {
                    String body = matcher.group(1), ext = matcher.group(2);               
                    System.out.println("Extension: " + body + "|" + ext);
                }
            }
            else System.out.println("NULL split for: "+res);
            
            System.out.println();
        }
    }        
        
}
