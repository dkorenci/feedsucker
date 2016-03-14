package feedsucker.core.messages;

/**
 * List of messages that FeedsuckerApp can receive, and logic
 * for turning these messages to corresponding exceptions.
 */
public class Messages {
    
    public static final String SHUTDOWN_NOW = "close";
    public static final String FINISH_AND_SHUTDOWN = "finish_and_close";
 
    public static boolean isShutdownMessage(String msg) {
        return (SHUTDOWN_NOW.equals(msg) || FINISH_AND_SHUTDOWN.equals(msg));
    }
    
    /** Throw matching exception for the message, if message is recognized. */
    public static void messageToException(String msg) 
            throws ShutdownException, FinishAndShutdownException {
        if (msg.equals(Messages.SHUTDOWN_NOW)) throw new ShutdownException();
        if (msg.equals(Messages.FINISH_AND_SHUTDOWN)) throw new FinishAndShutdownException();        
    }
    
}
