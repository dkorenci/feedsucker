package rsssucker.log;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.logging.XMLFormatter;

/** Creates and configures loggers. */
public class LoggersManager {
    
    private static String logFolder;           
    private static final Logger error;
    private static final Logger info;
    private static final Logger performance;
    private static final Logger debug;   
    private static final Logger errorUrl;
    
    // init logFolder and loggers
    static {
        Logger e = null, i = null, p = null, d = null, eu = null;
        try {
            createLogFolder();
            createRootLogger();
            e = createCategoryLoger("error");
            i = createCategoryLoger("info");
            p = createCategoryLoger("performance");
            d = createCategoryLoger("debug");  
            eu = createErrUrlLoger();
        } catch (Exception ex) {            
            outputLoggingErrorAndExit(ex);
        }
        finally {
            error = e;
            info = i;
            performance = p;
            debug = d;      
            errorUrl = eu;
        }
    }               

    // create folder for log files, named with current timestamp
    private static void createLogFolder() throws IOException {
        Date now = new Date();
        String format="yyyy-MM-dd_hh-mm-ss";
        String date = new SimpleDateFormat(format).format(now);
        logFolder="log/"+"log_"+date+"/";
        //System.out.println(folderName);
        File folder = new File(logFolder);
        boolean result = folder.mkdir();
        if (result == false) throw new IOException("Couldn't create log folder: "+logFolder);
    }
    
    private static void createRootLogger() throws IOException {
        Logger root = Logger.getLogger("");
        Handler[] handlers = root.getHandlers();
        for (Handler h : handlers) root.removeHandler(h); // remove all handlers
        // add file handler to the logger
        Handler h = new FileHandler(logFolder+"root.log");
        h.setFormatter(new MyXMLFormatter());
        root.addHandler(h);         
    }
    
    // create top level category logger for a given category
    private static Logger createCategoryLoger(String category) throws IOException {
        Logger logger = Logger.getLogger(category);
        logger.setUseParentHandlers(true); // enable message dispatch to parent (root)
        // remove all handlers, just in case
        Handler[] handlers = logger.getHandlers();
        for (Handler h : handlers) logger.removeHandler(h);
        // add file handler to the logger
        Handler h = new FileHandler(logFolder+category+".log");
        h.setFormatter(new MyXMLFormatter());
        logger.addHandler(h);        
        return logger;
    }
    
    // create top level category logger for a given category
    private static Logger createErrUrlLoger() throws IOException {
        Logger logger = Logger.getLogger("errorUrl");
        logger.setUseParentHandlers(false); // enable message dispatch to parent (root)
        // remove all handlers, just in case
        Handler[] handlers = logger.getHandlers();
        for (Handler h : handlers) logger.removeHandler(h);
        // add file handler to the logger
        Handler h = new FileHandler(logFolder+"errorUrl"+".log");
        h.setFormatter(new BareMessageFormatter());
        logger.addHandler(h);        
        return logger;
    }    
    
    // write error to stderr and terminate application
    private static void outputLoggingErrorAndExit(Exception ex) {
        System.err.println("error occured during initialization of logging: ");        
        ex.printStackTrace(System.err);
        System.exit(1);
    }    
        
    public static Logger getErrorLogger(String hierarchy) {
        return error;
    }
    
    public static Logger getErrorUrlLogger() {
        return errorUrl;
    }    

    public static Logger getDebugLogger(String hierarchy) {
        return debug;
    }
    
    public static Logger getInfoLogger(String hierarchy) {
        return info;
    }

    public static Logger getPerformanceLogger(String hierarchy) {
        return performance;
    }
    
}
