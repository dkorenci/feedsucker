package feedsucker.data;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

public class DataTests {

    public static void testEmfCreate() {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("feedsuckerPU");
        EntityManager em = emf.createEntityManager();                 
        em.getTransaction().begin();
        em.getTransaction().commit();
        em.close();
        emf.close();
    }
    
}
