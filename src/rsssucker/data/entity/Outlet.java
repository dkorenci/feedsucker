
package rsssucker.data.entity;

import java.util.Collection;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;

/**
 * Content producing outlet (news site, blog, ...).
 */
@Entity
public class Outlet {

    @Id
    @GeneratedValue
    private Long id;

    @Column(length=10000,unique = true)
    private String url;
    
    @Column(length=10000)
    private String attributes;    
    
    @OneToMany(mappedBy = "outlet")
    private Collection<Feed> feeds;
    
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id;}

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getAttributes() { return attributes; }
    public void setAttributes(String attributes) { this.attributes = attributes; }    

    public Collection<Feed> getFeeds() { return feeds; }
    public void setFeeds(Collection<Feed> feeds) { this.feeds = feeds; }
    
}
