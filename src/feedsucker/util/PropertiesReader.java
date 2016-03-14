package feedsucker.util;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/** Utility class for reading properties file and accessing properties. */
public class PropertiesReader {
    
    private Properties props;
    private String propertiesFile;
    
    public PropertiesReader(String file) throws IOException {
        propertiesFile = file;
        loadProperties();
    }
        
    private void loadProperties() throws IOException {
        props = new Properties();
        props.load(new FileInputStream(propertiesFile));
    }      
        
    public String getProperty(String name) {
        return props.getProperty(name);
    }

    // read integer property from properties file
    public int readIntProperty(String propName, int defaultValue) {
        int nt;
        try {
            nt = Integer.parseInt(getProperty(propName));
        } catch (NumberFormatException | NullPointerException e) {
            nt = defaultValue;
        }
        if (nt <= 0) {
            nt = defaultValue;
        }
        return nt;
    }
    
}
