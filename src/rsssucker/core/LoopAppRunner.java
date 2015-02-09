package rsssucker.core;

import java.io.BufferedReader;
import java.io.File;
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
import javautils.PropertiesReader;
import rsssucker.config.RssConfig;
import rsssucker.core.messages.FinishAndShutdownException;
import rsssucker.core.messages.MessageFileMonitor;
import rsssucker.core.messages.MessageReceiver;
import rsssucker.core.messages.ShutdownException;
import rsssucker.log.RssSuckerLogger;
import rsssucker.util.Timer;

/**
 * Runs RssSucker app and restart it at defined time intervals.
 */
public class LoopAppRunner {
    
    private static final int DEFAULT_RESTART_INTERVAL = 300; // in minutes    
    private static final long WAIT_FOR_SHUTDOWN =  30 * 1000; // in milis
    private static final long WAIT_BEFORE_STARTUP =  2 * 1000; // in milis
    private static final long WAIT_FOR_STARTUP =  1 * 1000; // in milis
    private static final long WAIT_AFTER_KILL =  10 * 1000; // in milis
    private static final String MESSAGE_FILE = "loop_messages.txt";
    
    private String javaBin; // folder with java runtime, compiler etc. 
    private int restartInterval;    
    
    private PropertiesReader properties;
    private MessageReceiver messageReceiver;
    private MessageFileMonitor messageMonitor;
    
    public LoopAppRunner(String java) { javaBin = java; }
    
    private static final RssSuckerLogger logger = 
            new RssSuckerLogger(LoopAppRunner.class.getName());    
    
    public void run() {
//        try {
//            processNotExist(5369, new Date());
//        } catch (Exception ex) {
//            Logger.getLogger(LoopAppRunner.class.getName()).log(Level.SEVERE, null, ex);
//        }
//        if (true) return;
        try {        
            
        initMessaging();
        readProperties();
        logger.info("restart interval: " + restartInterval);
        while (true) {
            // start new instance
            try {
                boolean result = startRsssucker();
                if (!result) {
                    logger.info("starting app failed - exiting");
                    break;
                }                
            } catch (IOException ex) {
                logger.info("!error starting app: " + ex.getMessage() + " , exiting");
                break;
            }            
            // sleep
            sleepCheck(restartInterval);
            // shutdown running instance
            try {          
                boolean result = shutdownRsssucker();
                if (result == false) {
                    logger.info("shutting down the app failed - exiting ");
                    break;
                }
            } catch (Exception ex) {
                logger.info("!error shutting the app down:\n" + ex.getMessage());
            }
        }
        
        }
        catch (ShutdownException | FinishAndShutdownException ex) {
             logger.info("MESSAGE INITIATED SHUTDOWN");
        }
        catch (Exception e) { logger.info("!error during execution:\n"+e.getMessage()); }        
        finally { // shutdown app and do cleanup
            logger.info("EXITING LOOP, START CLEANUP");
            try { shutdownRsssucker(); } catch (Exception ex) {
                logger.info("!error shutting the app down:\n" + ex.getMessage());                
            }
            cleanup();
        }
    }

    // cleanup resources before end
    private void cleanup() {
        messageMonitor.stop();                
    }
    
    private void initMessaging() {
        messageReceiver = new MessageReceiver();
        messageMonitor = new MessageFileMonitor(MESSAGE_FILE, messageReceiver);
        messageMonitor.start();
    }    
    
    private void readProperties() throws IOException {
        properties = new PropertiesReader(RssConfig.propertiesFile);
        restartInterval = 
         properties.readIntProperty("restart_interval", DEFAULT_RESTART_INTERVAL);   
        // turn minutes to miliseconds
        restartInterval *= 60 * 1000;
    }
    
    // try to shutdown app, return true if successful
    private boolean shutdownRsssucker() throws Exception {
        int pid = readPid(); // read current RssSucker instance's process id
        logger.info("ATTEMPTING TO SHUTDOWN APP");
        // try regular shutdown   
        Date now = new Date();
        try { execBashCommand("./rsssucker.sh STOP NOW"); }
        catch (IOException ex) {
            logger.info("executing STOP command failed:\n" + ex.getMessage());
        }        
        sleep(WAIT_FOR_SHUTDOWN);
        // if the process is still active, try to kill        
        if (!processNotExist(pid, now)) { 
            logger.info("could not shut down the app, killing it");
            now = new Date();
            killProcess(pid); 
            sleep(WAIT_AFTER_KILL);
            if (!processNotExist(pid, now)) {
                logger.info("app is not killed");
                return false;
            }
            else {
                logger.info("app is killed");
                return true;
            }
        }
        else { 
            logger.info("application is shut down");
            return true;
        }
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
        logger.info("checking existance of process with pid " + pid +
                " that started before " + d );
        String cmd = String.format("ps -p %d --no-headers -o lstart", pid);
        Process p = execBashCommand(cmd,"LC_TIME=en_US");
        BufferedReader out = new BufferedReader(new InputStreamReader(p.getInputStream()));
        // command returns start time on single line if process exists or empty output 
        String result = out.readLine();        
        if (result == null) { // no process
            logger.info("process with this pid does not exist");
            return true;
        } 
        else { // check process start date            
            // get start time, formatting should be: Wed Jan 28 10:06:04 2015            
            logger.info("process with this pid exists, checking time");
            logger.info("process start time read from ps output: " + result);
            DateFormat df = new SimpleDateFormat("EEE MMM dd HH:mm:ss yyyy", new Locale("en", "US"));
            Date date =  df.parse(result);                
            logger.info("process start time parsed: " + date + " , test date: " + d);
            if (date.after(d)) {
                logger.info("process did not start before " + d);
                return true;
            }
            else {
                logger.info("process started before " + d);
                return false;
            }
        }
    }
    
    // sleep at least designated number of milliseconds, 
    // continue sleep after each interrupt until the number is reached
    private void sleep(long milisToSleep) {        
        Timer timer;
        while (true) {
            timer = new Timer();
            try { Thread.sleep(milisToSleep+2); } 
            catch (InterruptedException ex) { logger.info("sleep interrupted"); }
            long elapsed = timer.milisFromStart();
            milisToSleep -= elapsed;
            if (milisToSleep <= 0) break;
        }        
    }
    
    // sleep at least designated number of milliseconds, periodically 
    // check for shutdown messages
    private void sleepCheck(long milisToSleep) 
            throws ShutdownException, FinishAndShutdownException {
        logger.info("STARTING SLEEP PERIOD");
        long sleepInterval = 200; // period between checks
        Timer timer;        
        while (true) {
            messageReceiver.checkMessages();
            timer = new Timer();
            try { Thread.sleep(sleepInterval); } 
            catch (InterruptedException ex) { logger.info("sleep interrupted"); }
            long elapsed = timer.milisFromStart();            
            milisToSleep -= elapsed;
            if (milisToSleep <= 0) break;
        }        
    }
    
    
    // execute rsssucker as a separate process, via startup script
    // return true iff started successfuly
    private boolean startRsssucker() throws IOException, ParseException { 
        logger.info("STARTING THE APP");
        String cmd = String.format("./rsssucker.sh %s %s", "START", javaBin);        
        deletePidFile();
        sleep(WAIT_BEFORE_STARTUP); // wait so that there is no collision with log files
        logger.info("executing: " + cmd);
        execBashCommand(cmd);
        sleep(WAIT_FOR_STARTUP); // give some time for the jvm with rsssucker to start running
        Date now = new Date(); 
        int pid;        
        try { pid = readPid(); }
        catch (Exception ex) { 
            logger.info("error reading process id");
            return false; 
        } // pid file does not exist
        // now check process itself, just to be sure
        if (processNotExist(pid, now)) { 
            logger.info("app did not start, process with id does not exist");
            return false;
        }
        else { 
            logger.info("app has started");
            return true;
        }
    }
    
    // run rsssucker, this execution shoudl correspond to run.sh script
    private Process execRsssucker(String java) throws IOException {
        String javaCmd;
        if ("default_java".equals(javaBin)) javaCmd = "java";
        else javaCmd = javaBin+java;
        ProcessBuilder pb = new ProcessBuilder(javaCmd, 
                "-jar -Xmx4g -XX:+UseConcMarkSweepGC RssSucker.jar > run_output.txt");
        Process p = pb.start();        
        return p;
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
