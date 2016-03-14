package feedsucker.article.newspaper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;
import javax.persistence.Query;
import feedsucker.article.ArticleData;
import feedsucker.data.Factory;
import feedsucker.data.JpaContext;
import feedsucker.data.entity.FeedArticle;

public class NewspaperTester {

    List<FeedArticle> articles;
    List<List<FeedArticle>> split;
    List<ArticleData> output;
    
    private class NewspaperRunnable implements Runnable {

        private List<ArticleData> output;
        private List<FeedArticle> articles;
        private Newspaper npaper;
        
        public NewspaperRunnable(String langCode) throws IOException 
        { 
            npaper = new Newspaper(langCode); 
        }
        
        public void setUrls(List<FeedArticle> art) { articles = art; }
        
        private void printUrl(String url) {
            synchronized(NewspaperRunnable.class) {
                System.out.println(Thread.currentThread().getId() + " scraped: " + url);
            }
        }
        
        @Override
        public void run() {
            output = new ArrayList<>();
            for (FeedArticle art : articles) {
                try {
                    ArticleData data = npaper.scrapeArticle(art.getUrl());
                    printUrl(art.getUrl());
                    output.add(data);
                } catch (Exception ex) {
                    System.out.println(ex.getMessage());
                    ArticleData data = new ArticleData();
                    data.setText(ex.getMessage());
                    data.setTitle("error");
                    output.add(data);
                }
            }
        }
        
        public List<ArticleData> getOutput() { return output; }                
    }    
    
    public void concurrencyTest(final int numThreads, final int numArticles) throws IOException, InterruptedException {
        articles = readArticles();
        if (numArticles < articles.size()) articles = articles.subList(0, numArticles);                    
        Collections.shuffle(articles, new Random(4567));
        System.out.println(articles.size());
        for (FeedArticle art : articles) {
            System.out.println(art.getFeedTitle());
        }
        // split article data
        createSplit(numThreads);
        // init threads and runnables
        NewspaperRunnable[] nprunner = new NewspaperRunnable[numThreads];
        Thread[] threads = new Thread[numThreads];
        for (int i = 0; i < numThreads; ++i) {
            nprunner[i] = new NewspaperRunnable("en");
            nprunner[i].setUrls(split.get(i));
            threads[i] = new Thread(nprunner[i]);
        }
        // start threads
        for (int i = 0; i < numThreads; ++i) threads[i].start();
        for (int i = 0; i < numThreads; ++i) threads[i].join();
        // test output correctness
        for (int i = 0; i < numThreads; ++i) {
            System.out.println("[Thread]: " + i);
            List<ArticleData> output = nprunner[i].getOutput();
            List<FeedArticle> articles = split.get(i);            
            System.out.println(
             String.format("output size %d , article size %d",output.size(), articles.size()) ); 
            if (output.size() != articles.size()) System.out.println("!!!! Size mismatch");
            else {
                int nonEqual = 0, equal = 0;
                for (int j = 0; j < output.size(); ++j) {
                    ArticleData downArt = output.get(j);
                    FeedArticle dbArt = articles.get(j);
                    System.out.println("[comparing articles] : ");
                    System.out.println(downArt.getTitle());
                    System.out.println(dbArt.getFeedTitle());
                    if (approximatelyEqual(downArt.getText(), dbArt.getText()) == false) {
                        nonEqual++;
                        System.out.println("[!!!! Article mismatch]");
                        System.out.println("[scraped] : "); System.out.println(downArt.getText());
                        System.out.println("[database]: "); System.out.println(dbArt.getText());
                    }
                    else equal++;
                }
                System.out.println("[total articles] : " + output.size() + 
                        " [equal] : " + equal + " [not equal] : " + nonEqual);
            }
        }
    }
    
    private static boolean approximatelyEqual(String s1, String s2) {
        final double perc = 0.1;
        double avglength = (s1.length() + s2.length())*0.5;
        double treshold = avglength * perc;
        int levDist = levenshteinDistance(s1, s2);
        System.out.println("[levenstein distance] : " + levDist +
               String.format(" [treshold] : %.3f ", treshold) + 
               String.format(" [avg_length] : %.3f ", avglength));
        if (levDist < treshold) return true;
        else return false;
    }    
    
    public static List<FeedArticle> readArticles() {
        JpaContext jpac = Factory.createContext();        
        Query q = jpac.em.createQuery("SELECT a FROM FeedArticle a");         
        List result = q.getResultList();                       
        jpac.close();        
        return (List<FeedArticle>)result;
    }
    
    public void createSplit(int parts) {
        int partSize = articles.size() / parts; 
        if (partSize == 0) partSize = 1;
        int start = 0, end = partSize; 
        split = new ArrayList<>();
        for (int i = 0; i < parts; ++i) {
            split.add(articles.subList(start, end));
            start = end;             
            end = start + partSize;
            if (i == parts-1) if (end < parts) end = parts;
        }
    }    

    
    public static int levenshteinDistance(String a, String b) {
        a = a.toLowerCase();
        b = b.toLowerCase();
        // i == 0
        int [] costs = new int [b.length() + 1];
        for (int j = 0; j < costs.length; j++)
            costs[j] = j;
        for (int i = 1; i <= a.length(); i++) {
            // j == 0; nw = lev(i - 1, j)
            costs[0] = i;
            int nw = i - 1;
            for (int j = 1; j <= b.length(); j++) {
                int cj = Math.min(1 + Math.min(costs[j], costs[j - 1]), a.charAt(i - 1) == b.charAt(j - 1) ? nw : nw + 1);
                nw = costs[j];
                costs[j] = cj;
            }
        }
        return costs[b.length()];
    }    
    
}
