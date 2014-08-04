package rsssucker.config;

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
    
}
