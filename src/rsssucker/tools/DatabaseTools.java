/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package rsssucker.tools;

import com.google.common.base.Function;
import com.google.common.escape.Escaper;
import com.google.common.escape.Escapers;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import javax.persistence.Query;
import rsssucker.data.Factory;
import rsssucker.data.JpaContext;
import rsssucker.data.entity.FeedArticle;

/**
 *
 * @author damir
 */
public class DatabaseTools {

    // list all hosts in saved articles
    private List<String> getHosts() throws URISyntaxException {
        List<FeedArticle> arts = getAllArticles();
        Set<String> hosts = new TreeSet<>();
        for (FeedArticle a : arts) {
            URI uri = new URI(a.getUrl());
            hosts.add(uri.getHost());
        }
        return new ArrayList<String>(hosts);
    }

    public void printHosts() throws URISyntaxException {
        List<String> hosts = getHosts();
        for (String h : hosts) System.out.println(h);
    }

    /** Export as table for statistical software (R, scipy, ...) */ 
    public void exportDatabaseAsTable() throws IOException {
        List<FeedArticle> articles = getAllArticles();
        BufferedWriter table = new BufferedWriter(new OutputStreamWriter(
            new FileOutputStream("table.txt"), "UTF-8"));                                
        char delimit = '\t';        
        // to make it readable for pandas read_table, quote strings and remove newlines
        Function<String,String> strProcess = new Function<String,String>() {
          private Escaper rmquote = Escapers.builder().addEscape('"', "").build();        
          private Escaper rmnewline = Escapers.builder().addEscape('\n', "").build();        
          @Override public String apply(String s) {
            s = rmquote.escape(s);
            return new StringBuilder(s.length()+2).append('"').append(s).append('"').toString();
          }
        };       
        table.append(tableHeader(delimit)).append("\n");
        for (FeedArticle art : articles) {
            table.append(toTableRow(art, delimit, strProcess)).append("\n");            
        }
        table.close();
    }
    
    private String tableHeader(char delimit) {
        StringBuilder b = new StringBuilder();
        b.append("id").append(delimit);
        b.append("date_published").append(delimit);
        b.append("date_saved").append(delimit);        
        b.append("url").append(delimit);
        b.append("author").append(delimit);        
        b.append("title_scraped").append(delimit);        
        b.append("title_feed").append(delimit);
        b.append("description").append(delimit);        
        b.append("text");    
        return b.toString();
    }
    
    // format article data for printing into a table row, include
    // modifier function for operations such as quoting and escaping
    private CharSequence toTableRow(FeedArticle art, char delimit, 
            Function<String, String> m) {
        StringBuilder b = new StringBuilder();
        b.append(art.getId()).append(delimit);
        b.append(art.getDatePublished()).append(delimit);
        b.append(art.getDateSaved()).append(delimit);        
        b.append(m.apply(art.getUrl())).append(delimit);
        b.append(m.apply(art.getAuthor())).append(delimit);        
        b.append(m.apply(art.getExtractedTitle())).append(delimit);        
        b.append(m.apply(art.getFeedTitle())).append(delimit);
        b.append(m.apply(art.getDescription())).append(delimit);        
        b.append(m.apply(art.getText()));
        return b;
    }    
    
    /** Return all articles from the database. Use config file db parameters. */
    public List<FeedArticle> getAllArticles()  {
        JpaContext ctx = null;    
        try {
            ctx = Factory.createContext();
            Query q = ctx.em.createNamedQuery("FeedArticle.getAll");
            List<FeedArticle> arts = (List<FeedArticle>)q.getResultList();
            return arts;
        }
        catch (Exception e) { throw e; }
        finally { if (ctx != null) ctx.close(); }     
    }
    
}
