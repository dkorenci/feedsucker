/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package rsssucker.data;

import java.util.Date;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Temporal;

/** Raw news article data downloaded from the web. */
@Entity
@NamedQueries({
    @NamedQuery(name = "getArticlesAfterDate", 
        query = "SELECT a FROM NewsArticle a WHERE a.datePublished >= :date")
})
public class NewsArticle {
    @Id
    @GeneratedValue
    private Long id;
    
    @Temporal(javax.persistence.TemporalType.DATE)
    private Date datePublished;
    
    @Column(length=100000)
    private String text;
    
    @Column(length=10000)
    private String description;
    
    /** Title that is given in the feed entry. */
    @Column(length=1000)
    private String feedTitle;
    
    /** Title extracted from downloaded article. */
    @Column(length=1000)
    private String extractedTitle;
    
    @Column(length=10000,unique = true)
    private String url;
    
    public Date getDatePublished() {
        return datePublished;
    }

    public void setDatePublished(Date datePublished) {
        this.datePublished = datePublished;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getFeedTitle() {
        return feedTitle;
    }

    public void setFeedTitle(String title) {
        this.feedTitle = title;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getExtractedTitle() {
        return extractedTitle;
    }

    public void setExtractedTitle(String extractedTitle) {
        this.extractedTitle = extractedTitle;
    }
    
}
