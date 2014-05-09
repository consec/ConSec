package org.consec.oauth2.authzserver.utils;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

public class PersistenceUtils {
    private static PersistenceUtils instance = null;
    private EntityManagerFactory emf;

    public static PersistenceUtils createInstance(String persistenceUnitName) {
        if (instance == null) {
            instance = new PersistenceUtils(persistenceUnitName);
            return instance;
        }
        else {
            throw new RuntimeException("Only one instance of PersistenceUtils is allowed.");
        }
    }

    public static PersistenceUtils getInstance() {
        return instance;
    }

    private PersistenceUtils(String persistenceUnitName) {
        emf = Persistence.createEntityManagerFactory(persistenceUnitName);
    }

    public EntityManager getEntityManager() {
        return emf.createEntityManager();
    }

    public void closeEntityManager(EntityManager em) {
        if (em != null) {
            em.close();
        }
    }

    public void close() {
        emf.close();
        instance = null;
    }
}
