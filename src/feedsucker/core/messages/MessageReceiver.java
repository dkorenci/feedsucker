package feedsucker.core.messages;

import java.util.ArrayDeque;
import java.util.Queue;

/** Store received messages on a queue and enable reading from the queue.
 * These operations are synchronized because of async communication with message producer.
 */
public class MessageReceiver implements IMessageReceiver {

    private final Queue<String> messageQueue;
    
    public MessageReceiver() {
        messageQueue = new ArrayDeque<>();
    }
    
    @Override
    public synchronized void receiveMessage(String msg) { messageQueue.add(msg); }  
    
    /** Returns next message from the message queue, or null if there are no messages. */
    private synchronized String readMessage() { return messageQueue.poll(); }    
    
    /** Check for message on top of the queue and signal it via exception.
     * Only messages understood by Messages class are signaled.
     */
    public void checkMessages() 
            throws ShutdownException, FinishAndShutdownException {
        String msg;
        while ((msg = readMessage()) != null) {
            Messages.messageToException(msg);
        }
    }
    
}
