package rsssucker.log;

import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Wrapper that contains all the necessary loggers and interface methods.
 */
public class RssSuckerLogger {
    
    private final Logger errLogger;            
    private final Logger infoLogger;  
    private final Logger errUrlLogger;
    
    public RssSuckerLogger(String className) {
        errLogger = LoggersManager.getErrorLogger(className);
        infoLogger = LoggersManager.getInfoLogger(className);   
        errUrlLogger = LoggersManager.getErrorUrlLogger();
    }
    
    public void logErr(String msg, Exception e) {
        errLogger.log(Level.SEVERE, msg, e);        
    }    
    
    public void logInfo(String msg, Exception e) {
        infoLogger.log(Level.INFO, msg, e);        
    }     
    
    public void logUrl(String url) {
        errUrlLogger.log(Level.INFO, url);
    }
    
    // send info message
    public void info(String msg) { 
        String header = String.format("[%1$td%1$tm%1$tY_%1$tH:%1$tM:%1$tS] : ", new Date());
        System.out.println(header + msg); 
    }       
    
}
