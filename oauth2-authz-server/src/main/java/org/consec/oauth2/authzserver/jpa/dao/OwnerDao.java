package org.consec.oauth2.authzserver.jpa.dao;

import org.apache.log4j.Logger;
import org.consec.oauth2.authzserver.jpa.entities.Owner;
import org.consec.oauth2.authzserver.jpa.enums.OwnerType;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;

public class OwnerDao extends BaseDao {
    protected static Logger log = Logger.getLogger(OwnerDao.class);

    public OwnerDao(EntityManager em) {
        super(em);
    }

    public Owner findByUuid(String uuid) {
        try {
            TypedQuery<Owner> q = em.createNamedQuery("Owner.findByUuid", Owner.class);
            q.setParameter("uuid", uuid);
            return q.getSingleResult();
        }
        catch (NoResultException e) {
            return null;
        }
    }

    public Owner getOwner(String ownerUuid) {
        Owner owner;
        owner = findByUuid(ownerUuid);
        if (owner == null) {
            log.trace("Owner is not yet registered at OAuth AS. Storing owner to the db.");
            owner = new Owner();
            owner.setUuid(ownerUuid);
            owner.setOwnerType(OwnerType.USER);

            em.getTransaction().begin();
            em.persist(owner);
            em.getTransaction().commit();
            log.trace("Owner has been registered successfully.");
        }

        return owner;
    }
}
