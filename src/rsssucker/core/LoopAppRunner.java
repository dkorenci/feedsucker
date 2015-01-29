package rsssucker.core;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import rsssucker.config.PropertiesReader;
import rsssucker.config.RssConfig;
import rsssucker.log.RssSuckerLogger;
import rsssucker.util.Timer;

/**
 * Runs RssSucker app and restart it at defined time intervals.
 */
public class LoopAppRunner {
    
    private static final int DEFAULT_RESTART_INTERVAL = 300; // in minutes
    private static final long WAIT_FOR_SHUTDOWN =  15 * 1000; // in milis
    
    private String javaBin; // folder with java runtime, compiler etc. 
    private int restartInterval;    
    
    private PropertiesReader properties;
    
    public LoopAppRunner(String java) { javaBin = java; }
    
    private static final RssSuckerLogger logger = 
            new RssSuckerLogger(RssSuckerApp.class.getName());    
    
    public void run() {
//        try {
//            processNotExist(5369, new Date());
//        } catch (Exception ex) {
//            Logger.getLogger(LoopAppRunner.class.getName()).log(Level.SEVERE, null, ex);
//        }
//        if (true) return;
        try {        
            
        readProperties();
            
        while (true) {
            // start new instance
            try {
                boolean result = startRsssucker();
                if (result) logger.info("rsssucker started ok");                
                else {
                    logger.info("starting rsssucker failed");
                    break;
                }                
            } catch (IOException ex) {
                logger.info("starting rsssucker failed:\n" + ex.getMessage());
                break;
            }            
            // sleep
            sleepNoInterrupt(restartInterval);
            // shutdown running instance
            try {          
                boolean result = shutdownRsssucker();
                if (result == false) {
                    logger.info("error shutting the app down");
                    break;
                }
            } catch (Exception ex) {
                logger.info("error shutting the app down:\n" + ex.getMessage());
            }
        }
        
        }
        catch (Exception e) { logger.info("error during loop execution:\n"+e.getMessage()); }
    }
    
    private void readProperties() throws IOException {
        properties = new PropertiesReader(RssConfig.propertiesFile);
        restartInterval = 
         properties.readIntProperty("restart_interval", DEFAULT_RESTART_INTERVAL);
        
    }
    
    // try to shutdown app, return true if successful
    private boolean shutdownRsssucker() throws Exception {
        int pid = readPid(); // read current RssSucker instance's process id
        // try regular shutdown   
        Date now = new Date();
        try { execBashCommand("./rsssucker.sh STOP NOW"); }
        catch (IOException ex) {
            logger.info("stopping rsssucker failed:\n" + ex.getMessage());
        }          
        sleepNoInterrupt(WAIT_FOR_SHUTDOWN);
        // if the process is still active, try to kill        
        if (!processNotExist(pid, now)) { 
            logger.info("app did not shut down, killing it");
            now = new Date();
            killProcess(pid); 
            if (!processNotExist(pid, now)) return false;
            else return true;
        }
        else return true;
    }
    
    // read rsssucker's process id from pid.txt (on first and only line of file)
    private int readPid() throws IOException {
        String line = new BufferedReader(new FileReader("pid.txt")).readLine();
        return Integer.parseInt(line);
    }
    
    private void killProcess(int pid) throws IOException {
        String cmd = String.format("kill -9 %d", pid);
        execBashCommand(cmd);
    }
    
    // return true if process with specified id that started before d doesnt exist, ie 
    // process with this id does not exist or exists but its start date is after d
    private boolean processNotExist(int pid, Date d) throws IOException, ParseException {
        String cmd = String.format("ps -p %d --no-headers -o lstart", pid);
        Process p = execBashCommand(cmd,"LC_TIME=en_US");
        BufferedReader out = new BufferedReader(new InputStreamReader(p.getInputStream()));
        // command returns start time on single line if process exists or empty output 
        String result = out.readLine();        
        if (result == null) return true; // no process
        else { // check process start date            
            // get start time, formatting should be: Wed Jan 28 10:06:04 2015            
            logger.info("resulting time: " + result);
            DateFormat df = new SimpleDateFormat("EEE MMM dd HH:mm:ss yyyy", new Locale("en", "US"));
            Date date =  df.parse(result);                
            System.out.println("process date: " + date + " , check date: " + d);
            if (date.after(d)) return true;
            else return false;
        }
    }
    
    // sleep at least designated number of milliseconds, 
    // continue sleep after each interrupt until the number is reached
    private void sleepNoInterrupt(long milis) {
        long toSleep = milis;
        Timer timer;
        while (true) {
            timer = new Timer();
            try { Thread.sleep(toSleep+5); } 
            catch (InterruptedException ex) { logger.info("sleep interrupted"); }
            long elapsed = timer.milisFromStart();
            toSleep -= elapsed;
            if (toSleep <= 0) break;
        }        
    }
    
    // execute rsssucker as a separate process, via startup script
    // return true iff started successfuly
    private boolean startRsssucker() throws IOException, ParseException {    
        String cmd = String.format("./rsssucker.sh %s %s", "START", javaBin);        
        deletePidFile();
        logger.info("executing: " + cmd);
        execBashCommand(cmd);
        sleepNoInterrupt(2000); // give some time for the jvm with rsssucker to start running
        Date now = new Date(); 
        int pid;        
        try { pid = readPid(); }
        catch (IOException ex) { return false; } // pid file does not exist
        // now check process itself, just to be sure
        if (processNotExist(pid, now)) return false;
        else return true;
    }
    
    private void deletePidFile() {
        File f = new File("pid.txt");
        f.delete();
    }
    
     private static Process execBashCommand(String cmd) throws IOException {
         return execBashCommand(cmd, null);
     }
    
    // run with specified environment variables set in format "VAR1=VAL1;VAR2=VAL2;..."
    private static Process execBashCommand(String cmd, String e) throws IOException {
        // return Runtime.getRuntime().exec(new String[]{"bash", "-c", cmd});           
        ProcessBuilder pb = new ProcessBuilder("bash", "-c", cmd);
        if (e != null) {
            Map<String, String> env = pb.environment();
            for (String assign : e.split(";")) 
                if ("".equals(assign) == false && assign.contains("=")) {                                        
                    String[] parts = assign.split("=");                           
                    env.put(parts[0], parts[1]);                
                }
        }            
        return pb.start();
    }    
    
}
