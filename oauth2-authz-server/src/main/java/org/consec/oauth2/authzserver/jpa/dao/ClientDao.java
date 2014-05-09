package org.consec.oauth2.authzserver.jpa.dao;

import org.consec.oauth2.authzserver.jpa.entities.Client;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.Query;

public class ClientDao extends BaseDao {

    public ClientDao(EntityManager em) {
        super(em);
    }

    public Client find(int clientId, int orgId) {
        Client client = em.find(Client.class, clientId);
        if (client == null || !client.getOrganization().getId().equals(orgId)) {
            return null;
        }
        else {
            return client;
        }
    }

    public Client findByClientId(String clientId) {
        try {
            Query q = em.createQuery("SELECT c FROM Client c WHERE c.clientId = :clientId");
            q.setParameter("clientId", clientId);
            return (Client) q.getSingleResult();
        }
        catch (NoResultException e) {
            return null;
        }
    }
}
