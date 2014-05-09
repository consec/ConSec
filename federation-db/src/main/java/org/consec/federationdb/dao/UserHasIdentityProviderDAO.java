package org.consec.federationdb.dao;

import org.consec.federationdb.model.UserHasIdentityProvider;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;

public class UserHasIdentityProviderDAO {

    private EntityManager em;

    public UserHasIdentityProviderDAO(EntityManager em) {
        this.em = em;
    }

    public UserHasIdentityProvider findByIdentity(String identity) {

        try {
            TypedQuery<UserHasIdentityProvider> query =
                    em.createNamedQuery("UserHasIdentityProvider.findByIdentity", UserHasIdentityProvider.class);
            query.setParameter("identity", identity);
            return query.getSingleResult();
        }
        catch (NoResultException e) {
            return null;
        }
    }
}
