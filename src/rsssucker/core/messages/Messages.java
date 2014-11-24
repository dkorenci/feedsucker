package rsssucker.core.messages;

/**
 * Messages that RssSuckerApp can receive
 */
public class Messages {
    public static final String SHUTDOWN_NOW = "close";
    public static final String FINISH_AND_SHUTDOWN = "finish_and_close";
 
    public static boolean isShutdownMessage(String msg) {
        return (SHUTDOWN_NOW.equals(msg) || FINISH_AND_SHUTDOWN.equals(msg));
    }
    
}
