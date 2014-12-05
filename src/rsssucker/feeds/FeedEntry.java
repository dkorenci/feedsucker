package rsssucker.feeds;

import com.sun.syndication.feed.synd.SyndEntry;
import java.util.Date;

/**
 * Feed entry data relevant for downloading and saving a news article.
 */
public class FeedEntry {

    private String url;
    private String redirUrl;
    private Date date;
    private String title;
    private String description;
    private String author;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
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

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }    

    public String getRedirUrl() {
        return redirUrl;
    }

    public void setRedirUrl(String redirUrl) {
        this.redirUrl = redirUrl;
    }
}
