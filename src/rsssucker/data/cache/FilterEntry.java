/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package rsssucker.data.cache;

import java.io.Serializable;
import java.util.Date;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Temporal;

/** Stores (feedUrl, articleUrl) pairs  */
@Entity
@NamedQueries({
    @NamedQuery(name = "FilterEntry.getAll", query = "SELECT fe FROM FilterEntry fe"),
    @NamedQuery(name = "FilterEntry.deleteOlder", 
                query = "DELETE FROM FilterEntry e WHERE e.date < :date")                
})
//@Table(indexes = {@Index(columnList = "datePublished", name = "datePubIndex")})
public class FilterEntry implements Serializable {
    
    @Id
    @Column(length=10000)
    private String feedUrl;
    
    @Id
    @Column(length=10000)    
    private String articleUrl;
    
    // time of storing in the cache
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    private Date date;

    public FilterEntry() {}
    
    public FilterEntry(String fUrl, String aUrl) {
        feedUrl = fUrl; articleUrl = aUrl; date = new Date();
    }
    
    
    private static final String SEPARATOR = "<|>";
    /** 1-1 maping to set of strings, used for searchable string representation. */
    public String getIdString() {
        return getIdString(getFeedUrl(), getArticleUrl());        
    }
       
    /** 1-1 maping from pair of urls to set of strings. */
    public static String getIdString(String feedUrl, String articleUrl) {
            return feedUrl + SEPARATOR + articleUrl;        
    }
        
    public String getFeedUrl() {
        return feedUrl;
    }
    public void setFeedUrl(String feedUrl) {
        this.feedUrl = feedUrl;
    }

    public String getArticleUrl() {
        return articleUrl;
    }
    public void setArticleUrl(String articleUrl) {
        this.articleUrl = articleUrl;
    }

    public Date getDate() {
        return date;
    }
    public void setDate(Date date) {
        this.date = date;
    }
    
    
}
