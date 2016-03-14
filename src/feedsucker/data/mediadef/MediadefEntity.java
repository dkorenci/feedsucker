package feedsucker.data.mediadef;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class MediadefEntity {

    public class Property {
        public String key, value;
        public Property(String k, String v) { key = k; value = v; }       
    }
    
    private final Map<String, String> keyToValue = new TreeMap<>();
    
    private final String klass;
    private final List<Property> properties;
        
    public MediadefEntity(String klass) {
        this.klass = klass.toLowerCase();
        properties = new ArrayList<>();
    }
    
    public void addProperty(Property p) { properties.add(p); }
    public void addProperty(String key, String value) { 
        key = key.toLowerCase();
        properties.add(new Property(key, value)); 
        keyToValue.put(key, value);
    }
    
    public String getKlass() { return klass; }
    public List<Property> getProperties() { return properties; }            
    public String getValue(String key) { return keyToValue.get(key); }
    
}
