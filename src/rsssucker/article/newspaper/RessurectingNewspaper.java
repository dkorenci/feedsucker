package rsssucker.article.newspaper;

import java.io.IOException;
import rsssucker.article.ArticleData;
import rsssucker.article.IArticleScraper;
import rsssucker.log.RssSuckerLogger;

/**
 * Wrapper around newspaper that initializes a new instance in case of failure.
 */
public class RessurectingNewspaper implements IArticleScraper {
    
    private Newspaper newspaper;
    private static final RssSuckerLogger logger = 
            new RssSuckerLogger(RessurectingNewspaper.class.getName());

    public RessurectingNewspaper() throws IOException {
        newspaper = new Newspaper();
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
            logger.logErr("newspaper crashed for url: " + articleUrl, e);
            try { newspaper.close(); }
            catch (Exception ex) { logger.logErr("closing newspaper failed for url", ex); }
            createNewspaper();            
        }
        }
        return null;
    }
    
    // try to create new newspaper
    private void createNewspaper() {
        try { newspaper = new Newspaper(); }
        catch (Exception ex) { 
            logger.logErr("creating new newspaper failed", ex); 
            newspaper = null;            
        }        
    }
    
    public synchronized void close() {
        if (newspaper != null) newspaper.close();
    }
            
}
