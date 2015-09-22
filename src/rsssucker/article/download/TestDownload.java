/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package rsssucker.article.download;

import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import org.jsoup.Jsoup;
import rsssucker.article.ArticleData;
import rsssucker.article.newspaper.Newspaper;
import rsssucker.article.newspaper.NewspaperException;

/**
 * Tests of various downloading methods.
 */
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
        
        Newspaper npaper = new Newspaper();
        String fileUrl = "http://localhost/test.html";
        ArticleData articleData = npaper.scrapeArticle(fileUrl);
        System.out.println(articleData.getText());        
        //System.out.println(html);        
    }
    
}
