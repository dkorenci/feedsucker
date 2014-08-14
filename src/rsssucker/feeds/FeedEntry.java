package rsssucker.feeds;

import com.sun.syndication.feed.synd.SyndEntry;
import java.util.Date;

/**
 * Feed entry data relevant for downloading and saving a news article.
 */
public class FeedEntry {

    private String articleURL;
    private Date date;
    private String title;
    private String description;
    
//    SyndEntry e; 
//    {
//        e.getTitle();
//    }

    public String getArticleURL() {
        return articleURL;
    }

    public void setArticleURL(String articleURL) {
        this.articleURL = articleURL;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
    
}
