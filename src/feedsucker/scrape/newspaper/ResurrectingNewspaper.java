package feedsucker.scrape.newspaper;

import java.io.IOException;
import feedsucker.scrape.ArticleData;
import feedsucker.scrape.IArticleScraper;
import feedsucker.log.FeedsuckerLogger;

/**
 * Wrapper around newspaper that initializes a new instance in case of a crash.
 */
public class ResurrectingNewspaper implements IArticleScraper {
    
    private Newspaper newspaper;
    private static final FeedsuckerLogger logger = 
            new FeedsuckerLogger(ResurrectingNewspaper.class.getName());    
    private final String language;
    
    public ResurrectingNewspaper(String langCode) throws IOException {        
        language = langCode;
        newspaper = new Newspaper(langCode);
    }
    
    // number of times to try fetching article (including first time
    // with original newspaper and subsequent tries with recreated newspaper)
    private static final int NUM_TRY = 2;
    
    @Override
    public synchronized ArticleData scrapeArticle(String articleUrl) 
            throws IOException, NewspaperException {
        ArticleData data = null;
        for (int i = 0; i < NUM_TRY; ++i) { 
        try {     
            if (newspaper != null) {
                data = newspaper.scrapeArticle(articleUrl);
                return data;
            }
            else createNewspaper();
        }
        catch (IOException e) {
            // IOException is for now only observed sign that the newspaper crashed
            logger.logUrl(articleUrl);
            logger.logErr("newspaper crashed for url: " + articleUrl, e);
            try { newspaper.close(); }
            catch (Exception ex) { logger.logErr("closing newspaper failed for url", ex); }
            createNewspaper();            
        }
        catch (NewspaperException e) {
            logger.logUrl(articleUrl);
            throw e;
        }
        }
        return null;
    }
    
    // try to create new newspaper
    private void createNewspaper() {
        try { newspaper = new Newspaper(language); }
        catch (Exception ex) { 
            logger.logErr("creating new newspaper failed", ex); 
            newspaper = null;            
        }        
    }
    
    public synchronized void close() {
        if (newspaper != null) newspaper.close();
    }
            
}
