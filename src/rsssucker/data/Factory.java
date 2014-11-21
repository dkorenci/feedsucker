package rsssucker.data;

import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;


public class Factory {

    public static EntityManagerFactory createEmf() {
        return Persistence.createEntityManagerFactory("rsssuckerPU");
    }
    
    public static JpaContext createContext() {
        JpaContext jpa = new JpaContext();
        jpa.emf = createEmf();
        jpa.em = jpa.emf.createEntityManager();        
        return jpa;
    }
    
}
