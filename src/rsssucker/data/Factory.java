package rsssucker.data;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javautils.PropertiesReader;
import rsssucker.config.RssConfig;


public class Factory {

    private static final String DB_UNAME_PROP = "db_username";
    private static final String DB_PASS_PROP = "db_password";
    private static final String DB_URL_PROP = "db_url";
    private static String dbUsername, dbPassword, dbUrl;
    
    static {
        try {
            readConnectionParams();
        } catch (Exception ex) {
            // errors are tolerated since the assumption is that if there is 
            // no config files, the params will be passed directly to createEmf()
        }
    }      
    
    /** Connect to postgresql database with connection parameters read from config file. */ 
    public static EntityManagerFactory createEmf() {
        if (dbUrl == null || dbUsername == null || dbPassword == null) {
            throw new IllegalArgumentException("not all database connection params"
                    + " (url, username, password) are specified");
        }
        return createEmf(dbUrl, dbUsername, dbPassword);
    }
    
    /** Connect to postgresql database at specified url, with specified username and password. */ 
    public static EntityManagerFactory createEmf(String url, String uname, String pass) {
        Map<String, Object> overrides = new HashMap<String, Object>();
        overrides.put("hibernate.connection.url", "jdbc:postgresql://" + url.trim());
        overrides.put("hibernate.connection.username", uname.trim());
        overrides.put("hibernate.connection.password", pass.trim());
        EntityManagerFactory emf =
            Persistence.createEntityManagerFactory("rsssuckerPU", overrides);                
        return emf;
    }
    
    public static JpaContext createContext() {
        JpaContext jpa = new JpaContext();
        jpa.emf = createEmf();
        jpa.em = jpa.emf.createEntityManager();        
        return jpa;
    }

    // read database connection params from config file
    private static void readConnectionParams() throws IOException {
        PropertiesReader properties = new PropertiesReader(RssConfig.propertiesFile);
        dbUrl = properties.getProperty(DB_URL_PROP);
        dbUsername = properties.getProperty(DB_UNAME_PROP);
        dbPassword = properties.getProperty(DB_PASS_PROP);
    }
    
}
