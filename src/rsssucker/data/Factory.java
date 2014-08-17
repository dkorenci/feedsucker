package rsssucker.data;

import javax.persistence.Persistence;


public class Factory {

    public static JpaContext createContext() {
        JpaContext jpa = new JpaContext();
        jpa.emf = Persistence.createEntityManagerFactory("rsssuckerPU");
        jpa.em = jpa.emf.createEntityManager();        
        return jpa;
    }
    
}
