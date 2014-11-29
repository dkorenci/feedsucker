/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package rsssucker.data.cache;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import javax.persistence.EntityExistsException;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Query;
import rsssucker.data.Factory;

/**
 * Functionality for filtering out already present (feed, article) pairs
 * to avoid scraping and persisting existent data. 
 * Filter is a singleton.
 */
public class Filter {
    
    private static Filter filter;
    
    private static final int ENTRY_LIFETIME = 3; // entry lifetime in days
    
    public static Filter getFilter() {
        if (filter == null) filter = createFilter();
        return filter;
    }
    
    public static void resetFilter() {        
        if (filter != null) filter.close();        
        filter = createFilter();
    }  
    
    public static void closeFilter() {
        if (filter != null) {
            filter.close();
            filter = null;
        }
    }
    
    private static Filter createFilter() { return new Filter(); }
    
    private EntityManagerFactory emf;
    private Set<String> entries;
    // new entries waiting to be written to DB
    private List<FilterEntry> pending;
    
    private Filter() {
        emf = Factory.createEmf();                
        loadFilterEntries();        
        pending = new ArrayList<>(20);
    }

    private synchronized void close() { emf.close(); }
       
    public synchronized boolean contains(String feedUrl, String articleUrl) {      
        return entries.contains(FilterEntry.getIdString(feedUrl, articleUrl));
    }
    
    public synchronized void addEntry(String feedUrl, String articleUrl) {
        String id = FilterEntry.getIdString(feedUrl, articleUrl);
        if (!entries.contains(id)) {
            entries.add(id);
            FilterEntry fe = new FilterEntry(feedUrl, articleUrl);
            pending.add(fe);
        } 
    }
    
    public synchronized void persistNewEntries() {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        for (FilterEntry fe : pending) {
            try { em.persist(fe); }
            catch (EntityExistsException ex) {}
        }
        em.getTransaction().commit();
    }
    
    public synchronized void removeExpiredEntries() {
        EntityManager em = emf.createEntityManager();
        Query q = em.createN amedQuery("FilterEntry.getAll");        
    }    
    
    private void loadFilterEntries() {
        EntityManager em = emf.createEntityManager();
        Query q = em.createNamedQuery("FilterEntry.getAll");
        List<FilterEntry> ent = q.getResultList();
        entries = new TreeSet<>();
        for (FilterEntry e : ent) entries.add(e.getIdString());
        em.close();
    }
}
