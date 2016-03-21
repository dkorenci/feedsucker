package feedsucker.data.entity;

import feedsucker.article.ArticleData;
import feedsucker.feedreader.FeedEntry;

/**
 * Utility methods for creating entity objects from various data. 
 */
public class EntityFactory {

    public static FeedArticle createFeedArticle(FeedEntry entry, ArticleData artData) {        
        FeedArticle article = new FeedArticle();        
        article.setDatePublished(entry.getDate());
        article.setExtractedTitle(artData.getTitle());
        article.setFeedTitle(entry.getTitle());
        article.setDescription(entry.getDescription());
        article.setText(artData.getText());
        article.setUrl(entry.getRedirUrl());    
        article.setAuthor(entry.getAuthor());
        return article;
    }
    
}
