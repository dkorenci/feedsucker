package rsssucker.data.mediadef;

import com.sun.syndication.io.FeedException;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import rsssucker.data.Factory;
import rsssucker.data.JpaContext;
import rsssucker.data.entity.Feed;
import rsssucker.data.entity.Outlet;
import rsssucker.feeds.RomeFeedReader;
import rsssucker.log.LoggersManager;

/**
 * Save mediadef entities and relations between them to database.
 */
public class MediadefPersister {
   
    private enum EntityType {FEED, OUTLET};
    
    private JpaContext jpa;
    private List<MediadefEntity> entities;
    // make all outlets in the persistence context fetchable by name
    private Map<String, Outlet> nameToOutlet = new TreeMap<>();
    // make all feed in the persistence context fetchable by url
    private Map<String, Feed> urlToFeed = new TreeMap<>();

    private static final Logger errLogger = 
            LoggersManager.getErrorLogger(MediadefPersister.class.getName());
    private static final Logger infoLogger = 
            LoggersManager.getInfoLogger(MediadefPersister.class.getName());      
    
    private static void logErr(String msg, Exception e) {
        errLogger.log(Level.SEVERE, msg, e);        
    }
    
    private static void logInfo(String msg, Exception e) {
        infoLogger.log(Level.INFO, msg, e);        
    }      
    
    /** Merge entity definitions to database: add unexisting entities, 
     * and update existing entities with new data. */
    public void persist(List<MediadefEntity> ents) throws MediadefException {
        entities = ents;
        // this is bulk processing of the entire mediadef file, the alternative
        // is a breakdown into atomic operations: process outlet, 
        // persisting each outlet to database separately and then
        // process feeds, persisting each feed to database separately
        jpa = Factory.createContext(); jpa.beginTransaction();        
        processOutlets();
        processFeeds();
        jpa.commitTransaction(); jpa.close();
    }
    
    /** Read outlets and feed from the database. */
    public static void printOutletsAndFeeds() {
        JpaContext jpa = Factory.createContext();
        Query q = jpa.em.createQuery("SELECT o FROM Outlet o");
        List<Outlet> outlets = (List<Outlet>) q.getResultList();
        for (Outlet o : outlets) {
            System.out.println(String.format("@Outlet name = %s , url = %s , atts = %s", 
                    o.getName(), o.getUrl(), o.getAttributes()));
            for (Feed f : o.getFeeds()) {
                System.out.println(String.format("\t@Feed ulr = %s , atts = %s", 
                        f.getUrl(), f.getAttributes()));                
            }
        }
        jpa.close();
    }
    
    // read outlets from mediadef, fetch from DB and update or create new
    private void processOutlets() throws MediadefException {
        for (MediadefEntity e : entities) if (e.getKlass().equals("outlet")) {            
            // TODO move checking of name existence to separate class
            String name = e.getValue("name").trim();
            if (name == null || name.equals("")) 
                throw new MediadefException("outlet must have a name property");
            Outlet outlet = (Outlet)getOrCreateEntity(EntityType.OUTLET, name, e);
            nameToOutlet.put(name, outlet);            
        }
    }            
    
    // read feeds from mediadef, fetch from DB and update or create new, 
    // update outlets with feeds
    private void processFeeds() throws MediadefException {
        for (MediadefEntity e : entities) if (e.getKlass().equals("feed")) {
            // TODO move checking of name existence to separate class
            String url = e.getValue("url").trim();
            if (url == null || url.equals("")) 
                throw new MediadefException("feed must have a url property");
            Feed feed = (Feed)getOrCreateEntity(EntityType.FEED, url, e);
            try { RomeFeedReader.readFeedData(feed); } 
            catch (FeedException|IOException ex) { logErr("feed data reading failed", ex); }
            urlToFeed.put(url, feed);
            // attach feed to outlet            
            String outletName = e.getValue("outlet");
            // TODO do detach feed from outlet, if necessary
            if (outletName != null && !outletName.equals("")) // name is defined
                attachFeedToOutlet(feed, outletName);
            else feed.setOutlet(null);
        }        
    }

    // set outlet property of the feed to outlet corresponding to outletName, 
    // the outlet may be in the database or a new outlet in the persistence context
    private void attachFeedToOutlet(Feed feed, String outletName) throws MediadefException {        
        Outlet outlet = null;
        if (nameToOutlet.containsKey(outletName)) outlet = nameToOutlet.get(outletName);
        else outlet = fetchOutletByName(outletName);
        if (outlet == null) {
            throw new MediadefException("outlet \""+outletName+
                    "\" does not exist for feed: " + feed.getUrl());
        }
        else { // connect outlet to feed                    
            // this association is unnecessary, at least with hibernate
            //outlet.getFeeds().add(feed); 
            feed.setOutlet(outlet);
            nameToOutlet.put(outletName, outlet);
        }        
    }
    
    // get entity by key either from the database or
    // create new entity and put into persistence context
    private Object getOrCreateEntity(EntityType type, String key, MediadefEntity e) 
            throws MediadefException {        
        // check for duplicate definitions
        // TODO move checking of duplication to separate class
        if (type == EntityType.OUTLET && nameToOutlet.containsKey(key)) 
            throw new MediadefException("duplicate definition of the outlet: " + key);
        if (type == EntityType.FEED && urlToFeed.containsKey(key)) 
            throw new MediadefException("duplicate definition of the feed: " + key);
        // try fetching from the database
        Object entity = null;
        if (type == EntityType.OUTLET) entity = fetchOutletByName(key);
        else if (type == EntityType.FEED) entity = fetchFeedByURL(key);     
        // create new entity or update existing
        if (entity == null) { // create new 
            if (type == EntityType.OUTLET) {
                entity = new Outlet();
                copyPropertiesToOutlet((Outlet)entity, e);
            }            
            else if (type == EntityType.FEED) {
                entity = new Feed();
                copyPropertiesToFeed((Feed)entity, e);
            }
            System.out.println("persisting: " + key);
            jpa.em.persist(entity);            
        }        
        else {
            // update with new data, entity is already managed, so changes will be
            // persisted at the and of the transaction
            if (type == EntityType.OUTLET) copyPropertiesToOutlet((Outlet)entity, e);                        
            else if (type == EntityType.FEED) copyPropertiesToFeed((Feed)entity, e);
        }   
        return entity;
    }      
    
    private void copyPropertiesToFeed(Feed feed, MediadefEntity e) {
        feed.setUrl(e.getValue("url"));
        feed.setAttributes(e.getValue("attributes"));
    }    
    
    private void copyPropertiesToOutlet(Outlet outlet, MediadefEntity e) {
        outlet.setName(e.getValue("name"));
        outlet.setAttributes(e.getValue("attributes"));
        outlet.setUrl(e.getValue("url"));
    }        
    
    // fetch feed from the database
    private Feed fetchFeedByURL(String url) {
        Query q = jpa.em.createNamedQuery("Feed.getByUrl");
        q.setParameter("url", url);  
        Object result;
        try { result = q.getSingleResult(); }
        catch (NoResultException ex) { return null; }
        return (Feed) result;
    }  
    
    // fetch outlet from the database
    private Outlet fetchOutletByName(String name) {
        Query q = jpa.em.createNamedQuery("Outlet.getByName");
        q.setParameter("name", name);  
        Object result;
        try { result = q.getSingleResult(); }
        catch (NoResultException ex) { return null; }
        return (Outlet) result;        
    }            
    
}
