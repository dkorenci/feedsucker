package rsssucker.data.mediadef;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Data structure for storage and methods for setting and querying mediadef data.
 */
public class MediadefData {

    Map<String, Map<String, Object>> data;
    
    public MediadefData() {
        data = new TreeMap<String, Map<String, Object>>();        
    }
    
    public String getEntityProperty(String id, String property) {
        Object o = data.get(id).get(property);
        if (o instanceof String) return (String)o;
        else throw new IllegalArgumentException(id+"."+property+" is not a (single) string");
    }
    public List<String> getEntityProperties(String id, String property) {
        Object o = data.get(id).get(property);
        if (o instanceof List) return Collections.unmodifiableList((List<String>)o);
        else throw new IllegalArgumentException(id+"."+property+" is not a list of strings");        
    }

    public void setEntityProperty(String id, String property, String value) {
        createMapSlot(id, property);
        data.get(id).put(property, value);
    }
    public void setEntityProperties(String id, String property, Collection<String> value) {
        createMapSlot(id, property);
        data.get(id).put(property, new ArrayList<String>(value));        
    }
    
    // create empty data.id.property slot if one does not exist
    private void createMapSlot(String id, String property) {
        if (data.containsKey(id) == false) {
            data.put(id, new TreeMap<String, Object>());
        }
        Map<String, Object> propMap = data.get(id);
        if (propMap.containsKey(property) == false) {
            propMap.put(property, null);
        }
    }
    
}
