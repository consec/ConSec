package org.consec.federationdb.dao;

import org.consec.federationdb.model.Attribute;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;

public class AttributeDAO {

    private EntityManager em;

    public AttributeDAO(EntityManager em) {
        this.em = em;
    }

    public Attribute findByName(String name) {

        try {
            TypedQuery<Attribute> query = em.createNamedQuery("Attribute.findByName", Attribute.class);
            query.setParameter("name", name);
            return query.getSingleResult();
        }
        catch (NoResultException e) {
            return null;
        }
    }
}
