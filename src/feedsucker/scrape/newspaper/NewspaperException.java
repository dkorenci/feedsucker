package feedsucker.scrape.newspaper;

/**
 * Exception signaling errors reported by newspaper API.
 * It occurs when newspaper reports error during normal operation
 * and the newspaper process itself has not crashed.
 */
public class NewspaperException extends Exception {
    private static final long serialVersionUID = -5551012728360146879L;

    public NewspaperException(String msg) {
        super(msg);
    }
    
}
