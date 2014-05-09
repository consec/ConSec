package org.consec.federationdb.dao;

import org.consec.federationdb.model.User;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;

public class UserDAO {

    private EntityManager em;

    public UserDAO(EntityManager em) {
        this.em = em;
    }

    public User findByEmail(String email) {

        try {
            TypedQuery<User> query = em.createNamedQuery("User.findByEmail", User.class);
            query.setParameter("email", email);
            return query.getSingleResult();
        }
        catch (NoResultException e) {
            return null;
        }
    }

    public User findByUsername(String username) {

        try {
            TypedQuery<User> query = em.createNamedQuery("User.findByUsername", User.class);
            query.setParameter("username", username);
            return query.getSingleResult();
        }
        catch (NoResultException e) {
            return null;
        }
    }
}
