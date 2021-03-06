package feedsucker.data;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

/**
 * Utility class, for accessing and closing EntityManagerFactory and EntityManager;
 */
public class JpaContext {

    EntityManagerFactory emf;
    public EntityManager em;
    
    public void close() {        
        em.close();
        emf.close();
    }
    
    public void beginTransaction() {
        em.getTransaction().begin();
    }
    
    public void commitTransaction() {
        em.getTransaction().commit();
    }
        
}
