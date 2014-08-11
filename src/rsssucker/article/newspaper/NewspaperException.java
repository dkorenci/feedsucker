package rsssucker.article.newspaper;

/**
 * Exception signaling errors reported by newspaper API.
 */
public class NewspaperException extends Exception {
    private static final long serialVersionUID = -5551012728360146879L;

    public NewspaperException(String msg) {
        super(msg);
    }
    
}
