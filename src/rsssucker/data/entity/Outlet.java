
package rsssucker.data.entity;

import java.util.ArrayList;
import java.util.Collection;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;

/**
 * Content producing outlet (news site, blog, ...).
 */
@Entity
@NamedQueries({
    @NamedQuery(name = "Outlet.getByName", 
            query = "SELECT o FROM Outlet o WHERE o.name = :name")
})
public class Outlet {

    @Id
    @GeneratedValue
    private Long id;

    @Column(length=100, unique = true)
    private String name;    
    
    @Column(length=10000, unique = true)
    private String url;
    
    @Column(length=10000)
    private String attributes;    
    
    @Column(length=10)
    private String language;        
    
    @OneToMany(mappedBy = "outlet")
    private Collection<Feed> feeds = new ArrayList<>();
    
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id;}

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }    
    
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getAttributes() { return attributes; }
    public void setAttributes(String attributes) { this.attributes = attributes; }    

    public Collection<Feed> getFeeds() { return feeds; }
    public void setFeeds(Collection<Feed> feeds) { this.feeds = feeds; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }
    
}
