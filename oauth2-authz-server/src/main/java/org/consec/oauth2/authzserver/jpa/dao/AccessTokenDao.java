package org.consec.oauth2.authzserver.jpa.dao;

import org.consec.oauth2.authzserver.jpa.entities.AccessToken;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.Query;

public class AccessTokenDao extends BaseDao {

    public AccessTokenDao(EntityManager em) {
        super(em);
    }

    public AccessToken findByToken(String token) {
        try {
            Query q = em.createQuery("SELECT t FROM AccessToken t WHERE t.token = :token");
            q.setParameter("token", token);
            return (AccessToken) q.getSingleResult();
        }
        catch (NoResultException e) {
            return null;
        }
    }
}
