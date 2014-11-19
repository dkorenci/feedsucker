package rsssucker.data.mediadef;

import java.util.ArrayList;
import java.util.List;

public class MediadefEntity {

    public class Property {
        public String key, value;
        public Property(String k, String v) { key = k; value = v; }       
    }
    
    private final String klass;
    private final List<Property> properties;
        
    public MediadefEntity(String klass) {
        this.klass = klass;
        properties = new ArrayList<>();
    }
    
    public void addProperty(Property p) { properties.add(p); }
    public void addProperty(String key, String value) { 
        properties.add(new Property(key, value)); 
    }
    
    public String getKlass() { return klass; }
    public List<Property> getProperties() { return properties; }            
    
}
