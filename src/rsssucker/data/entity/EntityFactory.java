package rsssucker.data.entity;

import rsssucker.article.ArticleData;
import rsssucker.feeds.FeedEntry;

public class EntityFactory {

    public static FeedArticle createFeedArticle(FeedEntry entry, ArticleData artData) {        
        FeedArticle article = new FeedArticle();        
        article.setDatePublished(entry.getDate());
        article.setExtractedTitle(artData.getTitle());
        article.setFeedTitle(entry.getTitle());
        article.setDescription(entry.getDescription());
        article.setText(artData.getText());
        article.setUrl(entry.getUrl());    
        article.setAuthor(entry.getAuthor());
        return article;
    }
    
}
