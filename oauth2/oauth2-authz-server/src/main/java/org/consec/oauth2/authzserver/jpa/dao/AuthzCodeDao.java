package org.consec.oauth2.authzserver.jpa.dao;

import org.consec.oauth2.authzserver.jpa.entities.AuthzCode;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.Query;

public class AuthzCodeDao extends BaseDao {

    public AuthzCodeDao(EntityManager em) {
        super(em);
    }

    public AuthzCode findByCode(String code) {
        try {
            Query q = em.createQuery("SELECT c FROM AuthzCode c WHERE c.code = :code");
            q.setParameter("code", code);
            return (AuthzCode) q.getSingleResult();
        }
        catch (NoResultException e) {
            return null;
        }
    }
}
