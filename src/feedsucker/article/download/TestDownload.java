package feedsucker.article.download;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import org.jsoup.Jsoup;
import feedsucker.article.ArticleData;
import feedsucker.article.newspaper.Newspaper;
import feedsucker.article.newspaper.NewspaperException;


public class TestDownload {

    public static void main(String[] args) throws IOException, NewspaperException {
        testJsoup();
    }
    
    public static void testJsoup() throws IOException, NewspaperException {
        String url = "http://www.nytimes.com/2015/09/17/opinion/the-elusive-truth-about-war-on-isis.html";
        String outFile = "/var/www/test.html";
        
        String html = Jsoup.connect(url).get().html();
        OutputStreamWriter writer = new OutputStreamWriter(
                new FileOutputStream(outFile), "utf8");
        writer.write(html);
        writer.close();
        
        Newspaper npaper = new Newspaper("en");
        String fileUrl = "http://localhost/test.html";
        ArticleData articleData = npaper.scrapeArticle(fileUrl);
        System.out.println(articleData.getText());        
        //System.out.println(html);        
    }
    
}
