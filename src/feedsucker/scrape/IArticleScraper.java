package feedsucker.scrape;

public interface IArticleScraper {

    public ArticleData scrapeArticle(String articleUrl) throws Exception;
    
}
