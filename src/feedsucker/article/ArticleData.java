package feedsucker.article;

/**
 * Scraped web article data.
 */
public class ArticleData {

    private String text; 
    private String title;

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
    
}
