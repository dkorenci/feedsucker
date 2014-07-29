/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package rsssucker.data;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

/**
 *
 */
public class DataOperations {

    public static void test() {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("rsssuckerPU");
        EntityManager em = emf.createEntityManager();                 
        em.getTransaction().begin();
        em.getTransaction().commit();
        em.close();
        emf.close();
    }
    
}
