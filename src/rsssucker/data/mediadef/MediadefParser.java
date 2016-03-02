package rsssucker.data.mediadef;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads file with media definitions. 
 * Mediadef file consists of blocks of type "@class" where class is 
 * entity class (feed, outlet, ...) followed by properties defined as
 * propName = "propValue" (apostrophes are optional but required if 
 * prop contains special characters or whitespace). 
 * Comments start with "//" and stretch till end of line.
 */
public class MediadefParser {
    
    private File file; 
    private String mediadef;
    
    public MediadefParser(String f) { this(new File(f)); }
    public MediadefParser(File f) { file = f; }

    // name of property that will designate entity class, the string after @
    private static String ENTITY_CLASS_PROP = "class";
    
    // entity header "@media, @feed, ..."
    private static String allowedEntityPunct = "\\-\\.\\_";    
    private static String entityRE = "@(\\p{Alpha}[\\p{Alnum}"+allowedEntityPunct+"]*)";
    private static Pattern entityPatt = Pattern.compile(entityRE);
    // property name followed by "=" sign
    private static String propNameRE = "(\\p{Alpha}\\p{Alnum}*)\\s*\\=";
    private static Pattern propNamePatt = Pattern.compile(propNameRE);
    // simple (alphanumeric, number) value of the property
    // TODO expand
    // allowed punctuation to include in prop value (when not using " as delimiters)
    private static String allowedPropPunct = 
            "!#\\$%\\&'\\(\\)\\*\\+\\,\\-\\./\\:<=>\\?@\\[\\\\\\]\\^\\_`{\\|}~";
    private static String propValueRE = "([\\p{Alnum}"+allowedPropPunct+"]+)(\\p{Space}*;)?";
    private static Pattern propValuePatt = Pattern.compile(propValueRE);
    // complex property value, anything between " "
    private static String propValueXRE = "\"([^\"]*)\"(\\p{Space}*;)?";
    private static Pattern propValueXPatt = Pattern.compile(propValueXRE);
    // whitespace
    private static String whitespaceRE = "\\p{Space}+";
    private static Pattern whitespacePatt = Pattern.compile(whitespaceRE);
    // comment, "//" and everything up to end of line
    private static String commentRE = "//.*\\n";
    private static Pattern commentPattern = Pattern.compile(commentRE);    
    
    Matcher entityMatcher, propMatcher, propValMatcher, propValXMatcher,
            commentMatcher, wsMatcher;
    
    int endPos; // last position of the mediadef string being processed
    int startPos; // position of the mediadef string to start matching at
    
    public List<MediadefEntity> parse() throws IOException, MediadefException {
        readFile();
        createMatchers();        
        startPos = 0; endPos = mediadef.length();       
        List<MediadefEntity> result = new ArrayList<>();
        while (true) {
            // parse entity header @entity
            stripWhitespaceAndComments();
            String entityClass = extractEntityHeader();            
            if (entityClass == null) { 
                if (end()) break;
                else throw new MediadefException("error parsing entity at position:\n" 
                        + mediadef.substring(startPos, endPos));
            }
            //System.out.println(entityClass);
            MediadefEntity entity = new MediadefEntity(entityClass);
            Map<String, String> properties = new TreeMap<String, String>();
            // parse parameters and values [param = (value || "value")]*
            while (true) {
                stripWhitespaceAndComments();
                String propName = extractPropertyName();                
                if (propName == null) break;
                if (propName.equals(ENTITY_CLASS_PROP)) {
                    throw new MediadefException("property " + propName + 
                            " is reserved for entity class");
                }
                //System.out.print(propName + " = ");
                stripWhitespaceAndComments();
                String propValue = extractPropertyValue();
                if (propValue == null) {
                    throw new MediadefException(
                    "error parsing propertyValue at position:\n" 
                    + mediadef.substring(startPos, startPos+100));
                }
                properties.put(propName, propValue);
                //System.out.println(propValue);
                entity.addProperty(propName, propValue);
            }
            result.add(entity);
        }        
        return result;
    }

    private boolean end() { return startPos == endPos; }
    
    private String extractEntityHeader() {
        entityMatcher.region(startPos, endPos);
        if (entityMatcher.lookingAt()) {
            startPos = entityMatcher.end();
            return entityMatcher.group(1);            
        }
        else return null;
    }

    private String extractPropertyName() {
        propMatcher.region(startPos, endPos);
        if (propMatcher.lookingAt()) {
            startPos = propMatcher.end();
            return propMatcher.group(1);
        }
        else return null;        
    }    

    private String extractPropertyValue() {
        propValMatcher.region(startPos, endPos);
        propValXMatcher.region(startPos, endPos);
        if (propValMatcher.lookingAt()) {
            startPos = propValMatcher.end();
            return propValMatcher.group(1);
        }
        else if (propValXMatcher.lookingAt()) {
            startPos = propValXMatcher.end();
            return propValXMatcher.group(1);            
        }
        else return null;
    }    
        
    private void stripWhitespaceAndComments() {       
        boolean commentMatch;
        //System.out.println(">>>>"+mediadef.subSequence(startPos, startPos+10));
        do {                        
            wsMatcher.region(startPos, endPos);        
            boolean wsMatch = wsMatcher.lookingAt();            
            //if (wsMatch) System.out.println("|"+wsMatcher.group(0)+"|");
            if (wsMatch) startPos = wsMatcher.end();
            commentMatcher.region(startPos, endPos);            
            commentMatch = commentMatcher.lookingAt();      
            //if (commentMatch) System.out.println("|"+commentMatcher.group(0)+"|");
            if (commentMatch) startPos = commentMatcher.end();
        } while (commentMatch);
    }    
    
    private void createMatchers() {
        entityMatcher = entityPatt.matcher(mediadef);
        propMatcher = propNamePatt.matcher(mediadef);
        propValMatcher = propValuePatt.matcher(mediadef);
        propValXMatcher = propValueXPatt.matcher(mediadef);        
        wsMatcher = whitespacePatt.matcher(mediadef);
        commentMatcher = commentPattern.matcher(mediadef);
    }
    
    
    private void readFile() throws IOException {
        StringBuilder buffer = new StringBuilder();
        BufferedReader reader = new BufferedReader(new FileReader(file));
        String line;
        while ((line = reader.readLine()) != null) {
            buffer.append(line).append("\n");
        }
        mediadef = buffer.toString();
    }
    
}
