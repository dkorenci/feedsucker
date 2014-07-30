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
import javax.persistence.Temporal;

/** Raw news article data downloaded from the web. */
@Entity
public class NewsArticle {

    @Id
    @GeneratedValue
    private Long id;
    
    @Temporal(javax.persistence.TemporalType.DATE)
    private Date datePublished;
    
    @Column(length=100000)
    private String text;
    
    @Column(length=10000)
    private String summary;
    
    @Column(length=1000)
    private String title;
    
    @Column(length=10000)
    String url;
    
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

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
    
}
