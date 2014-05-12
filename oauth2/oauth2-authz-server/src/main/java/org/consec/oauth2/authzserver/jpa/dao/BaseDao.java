package org.consec.oauth2.authzserver.jpa.dao;

import javax.persistence.EntityManager;

public abstract class BaseDao {
    protected EntityManager em;

    protected BaseDao(EntityManager em) {
        this.em = em;
    }
}
