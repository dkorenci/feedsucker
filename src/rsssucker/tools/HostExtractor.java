package rsssucker.tools;

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

public class HostExtractor {

    public static void printHosts() {
        try {
            List<String> hosts = getHosts();
            for (String h : hosts) System.out.println(h);
        }
        catch (Exception e) {
            System.out.println("ERROR: ");
            e.printStackTrace(System.out);
        }
    }
    
    // list all hosts in saved articles
    private static List<String> getHosts() throws URISyntaxException {
        JpaContext ctx = null;
        try {
            ctx = Factory.createContext();
            Query q = ctx.em.createNamedQuery("FeedArticle.getAll");
            List<FeedArticle> arts = (List<FeedArticle>)q.getResultList();
            Set<String> hosts = new TreeSet<>();
            for (FeedArticle a : arts) {
                URI uri = new URI(a.getUrl());
                hosts.add(uri.getHost());                
            }
            return new ArrayList<>(hosts);
        }
        finally { if (ctx != null) ctx.close(); }
    }
    
}
