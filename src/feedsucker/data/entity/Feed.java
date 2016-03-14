package feedsucker.data.entity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;

/**
 * Information about a single news feed.
 */
@Entity
@NamedQueries({
    @NamedQuery(name = "Feed.getByUrl", query = "SELECT f FROM Feed f WHERE f.url = :url"),
    @NamedQuery(name = "Feed.getAll", query = "SELECT f FROM Feed f")
})
public class Feed {
    
    // feed type codes
    public static final String TYPE_SYNDICATION = "synd";
    public static final String TYPE_WEBPAGE = "html";
    public static final String[] types = {TYPE_SYNDICATION, TYPE_WEBPAGE};
    public static final Set<String> validTypes = new TreeSet<>(Arrays.asList(types));
    
    public static boolean validTypeCode(String code) {
        return validTypes.contains(code);
    }
    
    @Id
    @GeneratedValue
    private Long id;

    @ManyToOne
    private Outlet outlet;
    
    @Column(length=10000,unique = true)
    private String url;
    
    @Column(length=200)
    private String title;
    
    @Column(length=1000)
    private String description;
    
    @Column(length=10000)
    private String attributes;    
        
    @Column(length=10)
    private String language;    

    // describe weather it is synd (rss/atom), html, ...
    @Column(length=50)
    private String type;        
    
    @Basic(fetch=FetchType.LAZY)
    @ManyToMany(mappedBy = "feeds")
    private Collection<FeedArticle> articles = new ArrayList<>();
    
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

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    
}
