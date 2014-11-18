package rsssucker.data.entity;

import java.util.Collection;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;

/**
 * Information about a single news feed.
 */
@Entity
public class Feed {
    @Id
    @GeneratedValue
    private Long id;

    @ManyToOne
    private Outlet outlet;
    
    @Column(length=10000,unique = true)
    private String url;
    
    @Column(length=10000)
    private String attributes;    
    
    @ManyToMany
    private Collection<FeedArticle> articles;
    
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id;}

    public Outlet getOutlet() { return outlet; }
    public void setOutlet(Outlet outlet) { this.outlet = outlet; }
    
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getAttributes() { return attributes; }
    public void setAttributes(String attributes) { this.attributes = attributes; }

    public Collection<FeedArticle> getArticles() { return articles; }
    public void setArticles(Collection<FeedArticle> articles) { this.articles = articles; }
    
}
