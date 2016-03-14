package feedsucker.article;

public interface IArticleScraper {

    public ArticleData scrapeArticle(String articleUrl) throws Exception;
    
}
