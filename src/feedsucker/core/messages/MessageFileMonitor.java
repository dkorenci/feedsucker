package feedsucker.core.messages;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import feedsucker.log.FeedsuckerLogger;

/**
 * Monitors a file for text messages and when they are received, 
 * send them to IMessageReceiver. Each monitor must have its own file.
 */
public class MessageFileMonitor implements Runnable {

    private IMessageReceiver receiver;
    
    private static final FeedsuckerLogger logger = 
            new FeedsuckerLogger(MessageFileMonitor.class.getName());     
    
    // file checking interval, in milis
    private static final int REFRESH_INTERVAL = 1 * 1000;
    // message file
    private final String messageFile;
    
    private Thread runnerThread = null;
    private boolean stop; // flag to signal stopping the run
    
    public MessageFileMonitor(String file, IMessageReceiver r) {         
        this.messageFile = file;
        this.receiver = r; 
    }
     
    @Override
    /** This is entry point for a thread. Monitor must be started with start() method. */
    public void run() {
        logger.logInfo("Message Monitor starting", null);
        while (true) {
            if (stop) break;
            String msg = readMessage();
            if (msg != null) sendMessage(msg);
            try { Thread.sleep(REFRESH_INTERVAL); } 
            catch (InterruptedException ex) { }
        }
    }   
    
    /** Start new thread running the monitor, if it is not already running.  */
    public void start() {
        if (runnerThread == null || !runnerThread.isAlive()) {
            deleteMessageFile();
            stop = false;
            runnerThread = new Thread(this); 
            runnerThread.start();
        }
    }
    /** If the monitor is running in a thread, send stop signal. */
    public void stop() {
        if (runnerThread != null && runnerThread.isAlive()) {
            stop = true;
            runnerThread.interrupt();
        }
    }
    
    // check messageFile for shutdownMessages and returns the first one read
    // if no messages are found or an error occurs, return null
    private String readMessage() {
        BufferedReader reader = null;
        try {
            Path path = Paths.get(messageFile);        
            if (Files.exists(path)) {
                reader = Files.newBufferedReader(path, 
                        StandardCharsets.US_ASCII);
                String line;
                while ((line = reader.readLine()) != null) {
                    String msg = line.trim().toLowerCase();
                    if (Messages.isShutdownMessage(msg)) {
                        reader.close(); Files.delete(path);
                        return msg;                        
                    }
                }
                // no shutdown messages found                
                reader.close(); Files.delete(path);
                return null;
            }
            else return null;
        } catch (Exception e) {
            logger.logErr("message file processing error", e);
            return null;
        }
        finally {
            try { if (reader != null) reader.close();  } 
            catch (IOException ex) { logger.logErr("buffered reader close error", ex); }                            
        }
    }

    private void deleteMessageFile() {
        Path path = Paths.get(messageFile); 
        if (Files.exists(path)) {
            try { Files.delete(path); } 
            catch (IOException ex) { logger.logErr("error deleting message file", ex); }
        }
    }
    
    // send message to the receiver
    private void sendMessage(String msg) {
        receiver.receiveMessage(msg);
    }
    
}
