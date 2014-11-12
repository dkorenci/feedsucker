package rsssucker.data.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

/**
 * Information about a single news feed.
 */
@Entity
public class Feed {
    @Id
    @GeneratedValue
    private Long id;

    @Column(length=10000,unique = true)
    private String url;
    
    @Column(length=10000)
    private String attributes;    
    
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id;}

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getAttributes() { return attributes; }
    public void setAttributes(String attributes) { this.attributes = attributes; }
    
}
