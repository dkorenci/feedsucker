package feedsucker.article.download;

import java.io.IOException;
import org.jsoup.Jsoup;

/**
 * Download web page html using Jsoup library
 */
public class JsoupHtmlDownloader implements IHtmlDownloader {

    @Override
    public String downloadHtml(String url) throws IOException {
          String html = Jsoup.connect(url).get().html();
          return html;
    }
    
}
