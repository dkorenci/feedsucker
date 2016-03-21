package feedsucker.feeds.html;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import feedsucker.feeds.FeedEntry;
import feedsucker.feeds.IFeedReader;

/**
 * View web page as a feed, URLs in the feeds are URLs of articles published on the page.
 * Article URLs that are extracted are children of feed/page URL whose URL path 
 * ends in an article title - list of words separated by non alphabetic characters.
 * List of words and threshold for the number of words are configurable
 * (ie words from a particular language).
 */
public class HtmlFeedReader implements IFeedReader {

    private String feedUrl;
    private Set<String> words;
    private int wordTreshold;
    
    /**     
     * @param words dictionary to decide weather url ends in a title (list of words)
     * @param treshold minimum number of words in the ending for the url to be considered a title
     */
    public HtmlFeedReader(Set<String> words, int treshold) {
        this.words = words; wordTreshold = treshold;
    }
    
    @Override
    public List<FeedEntry> getFeedEntries(String feedUrl) throws Exception {
        this.feedUrl = feedUrl;
        Document doc = Jsoup.connect(feedUrl).get();           
        URI baseUri = new URI(doc.baseUri());
        Elements anchors = doc.select("a[href]");        
        List<FeedEntry> result = new ArrayList<>();
        Set<String> urlSet = new TreeSet<>();
        for (Element elem : anchors) {                    
            String link = elem.attr("href");                        
            URI uri = new URI(link);            
            URI res = baseUri.resolve(uri);               
            if (selectLink(res)) {
                if (urlSet.contains(res.toString()) == false) {
                    result.add(linkToFeedEntry(res));
                    urlSet.add(res.toString());
                }
            }
        }        
        return result;
    }
    
    public List<String> getAllLinks(String feedUrl) throws Exception {
        this.feedUrl = feedUrl;
        Document doc = Jsoup.connect(feedUrl).get();           
        URI baseUri = new URI(doc.baseUri());
        Elements anchors = doc.select("a[href]");        
        List<String> result = new ArrayList<>();
        for (Element elem : anchors) {                    
            String link = elem.attr("href");                        
            URI uri = new URI(link);            
            URI res = baseUri.resolve(uri);               
            result.add(res.toString());
        }        
        return result;
    }

    
    /**
     * Return true if link is considered a feed link.
     * @param link absolute link parsed to URI
     * @return 
     */
    private boolean selectLink(URI link) throws UnsupportedEncodingException, URISyntaxException {        
        if (isChildOfTheFeed(link) == false) return false;        
        String path = link.getPath();
        if (path == null) return false;
        if ("".equals(path) || "/".equals(path)) return false;
        String [] split = path.split("/");
        if (split.length == 0) return false;
        String endOfPath = split[split.length-1];
        endOfPath = URLDecoder.decode(endOfPath, "UTF-8"); // decode % coded chars
        // lowercasing is neccessary for subsequent steps
        endOfPath = endOfPath.trim().toLowerCase(); 
        if (forbiddenFileType(endOfPath)) return false;
        return isTitle(endOfPath);        
    }

    /**
     * True if the words (alphabetic sequences) in the path are words in the dictionary. 
     * @param endOfPath last part of a html link path     
     */
    private boolean isTitle(String endOfPath) {
        endOfPath = removeFileExtension(endOfPath);
        String[] alphaTokens = endOfPath.split("[^a-z]+");
        if (this.words == null) {
            // no dictionary present, consider all the tokens to be words
            return alphaTokens.length >= this.wordTreshold;
        }
        else { // count tokens that appear in the dictionary
            int wordTokens = 0;
            for (String tok : alphaTokens)
                if (this.words.contains(tok)) wordTokens++;            
            return wordTokens >= this.wordTreshold;
        }        
    }
        
    private FeedEntry linkToFeedEntry(URI res) {
        FeedEntry e = new FeedEntry();
        e.setUrl(res.toString());
        e.setRedirUrl(res.toString());
        return e;
    }

    // list of forbidden extensions, taken form default scrapy config file.
    private String [] exts = {
        // images
        "mng", "pct", "bmp", "gif", "jpg", "jpeg", "png", "pst", "psp", "tif",
        "tiff", "ai", "drw", "dxf", "eps", "ps", "svg",
        // audio
        "mp3", "wma", "ogg", "wav", "ra", "aac", "mid", "au", "aiff",
        // video
        "3gp", "asf", "asx", "avi", "mov", "mp4", "mpg", "qt", "rm", "swf", "wmv",
        "m4a",
        // office suites
        "xls", "xlsx", "ppt", "pptx", "doc", "docx", "odt", "ods", "odg", "odp",
        // other
        "css", "pdf", "exe", "bin", "rss", "zip", "rar",
    };    
    private Set<String> extset = new TreeSet<String>(Arrays.asList(exts)); 
    // pattern describing a file name with extension, with capturing groups
    private Pattern extPattern = Pattern.compile("^(.*)\\.([a-z]+)$");
    
    /**
     * True if string ends with forbiddend file extension.
     * @param endOfPath last part of a html link path       
     */
    private boolean forbiddenFileType(String endOfPath) {          
        Matcher matcher = extPattern.matcher(endOfPath);                
        if (matcher.matches()) {
            String ext = matcher.group(2); 
            if (extset.contains(ext)) return true;
            else return false;
        } else return false;        
    }

    /**
     * @param endOfPath last part of a html link path  
     * @return 
     */
    private String removeFileExtension(String endOfPath) {        
        Matcher matcher = extPattern.matcher(endOfPath);                
        if (matcher.matches()) return matcher.group(1);
        else return endOfPath;
    }

    // return true if the link is a child of current feed's URI
    // http URIs are assumed
    private boolean isChildOfTheFeed(URI link) throws URISyntaxException {
        URI feedUri = new URI(this.feedUrl);
        return isChildOf(feedUri, link);
    }

    // return true if one URI is a child of another URI
    // http URIs are assumed    
    private boolean isChildOf(URI parent, URI child) throws URISyntaxException {  
        // TODO this is too permissive, '/' separated "folders" should be checked                
        return child.toString().startsWith(parent.toString());
        // solution that compares parts of the parsed URI
//        if (!stringEqual(parent.getAuthority(), child.getAuthority())) return false;
//        if (!stringEqual(parent.getHost(), child.getHost())) return false;
//        if (parent.getPort() != child.getPort()) return false;        
//        String pp = parent.getPath(), cp = child.getPath();
//        if (pp == null) return cp == null; // TODO is this correct?
//        if (cp == null) return false;
//        return cp.startsWith(pp);         
    }
    
    private static boolean stringEqual(String s1, String s2) {
        if (s1 == null) return s2 == null;
        else return s1.equals(s2);
    }
    
}
