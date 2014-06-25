package org.consec.dynamicca.jpa;

import org.apache.log4j.Logger;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

public class EMF {
    private static final String DEFAULT_PERSISTENCE_UNIT = "defaultPersistenceUnit";
    protected static Logger log = Logger.getLogger(EMF.class);
    private static EntityManagerFactory emf;

    public static void init() throws Exception {
        init(DEFAULT_PERSISTENCE_UNIT);
    }

    public static void init(String persistenceUnitName) throws Exception {
        try {
            emf = Persistence.createEntityManagerFactory(persistenceUnitName);
            log.info("JPA context initialized successfully.");
        }
        catch (Exception e) {
            log.error("Failed to initialize JPA context: " + e.getMessage(), e);
            throw new Exception("Failed to initialize JPA context.", e);
        }
    }

    public static void close() {
        emf.close();
        log.debug("JPA context closed successfully.");
    }

    public static EntityManager createEntityManager() {
        if (emf == null) {
            throw new IllegalStateException("Context is not initialized yet.");
        }

        return emf.createEntityManager();
    }

    public static void closeEntityManager(EntityManager em) {
        if (em != null) {
            em.close();
        }
    }
}
