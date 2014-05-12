package org.consec.oauth2.authzserver.utils;

import org.consec.oauth2.authzserver.jpa.entities.*;
import org.consec.oauth2.authzserver.jpa.enums.ClientTrustLevel;
import org.consec.oauth2.authzserver.jpa.enums.OrganizationTrustLevel;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.util.List;

public class TrustUtils {

    private EntityManager em;

    public TrustUtils(EntityManager em) {
        this.em = em;
    }

    public Boolean isTrusted(Owner owner, Client client) {

        OrganizationTrust orgTrust = em.find(OrganizationTrust.class,
                new OrganizationTrustPK(owner.getId(), client.getOrganization().getId()));

        if (orgTrust == null || orgTrust.getTrustLevel() == OrganizationTrustLevel.PARTLY) {
            // check if client is trusted
            ClientTrust clientTrust = em.find(ClientTrust.class,
                    new ClientTrustPK(owner.getId(), client.getId()));
            if (clientTrust == null) {
                return null;
            }
            else if (clientTrust.getTrustLevel() == ClientTrustLevel.NOT_TRUSTED) {
                return false;
            }
            else if (clientTrust.getTrustLevel() == ClientTrustLevel.TRUSTED) {
                return areCountriesTrusted(owner, client.getCountryList());
            }
            else {
                throw new RuntimeException("Invalid ClientTrustLevel: " + clientTrust.getTrustLevel());
            }
        }
        else if (orgTrust.getTrustLevel() == OrganizationTrustLevel.DENIED) {
            return false;
        }
        else if (orgTrust.getTrustLevel() == OrganizationTrustLevel.FULLY) {
            return areCountriesTrusted(owner, client.getCountryList());
        }
        else {
            throw new RuntimeException("Invalid OrganizationTrustLevel: " + orgTrust.getTrustLevel());
        }
    }

    private Boolean areCountriesTrusted(Owner owner, List<Country> countries) {
        if (owner.getCountryRestriction() == false) {
            return true;
        }
        else
            if (countries == null) {
                return null;
            }
            for (Country country : countries) {
                CountryTrust countryTrust = em.find(CountryTrust.class,
                        new CountryTrustPK(owner.getId(), country.getCode()));
                if (countryTrust == null) {
                    return null;
                }
                else if (!countryTrust.getIsTrusted()) {
                    return false;
                }
            }
        return true;
    }

    public void setClientTrust(Owner owner, Client client, ClientTrustLevel trustLevel) {
        EntityManager em = PersistenceUtils.getInstance().getEntityManager();
        try {

            ClientTrust clientTrust = em.find(ClientTrust.class,
                    new ClientTrustPK(owner.getId(), client.getId()));

            if (clientTrust == null) {
                clientTrust = new ClientTrust(owner.getId(), client.getId());
                clientTrust.setTrustLevel(trustLevel);
                em.getTransaction().begin();
                em.persist(clientTrust);
                em.getTransaction().commit();
            }
            else {
                clientTrust.setTrustLevel(trustLevel);
                em.getTransaction().begin();
                em.merge(clientTrust);
                em.getTransaction().commit();
            }
        }
        finally {
            PersistenceUtils.getInstance().closeEntityManager(em);
        }
    }

    public void revokeAccessTokens(Owner owner, Client client) {

        TypedQuery<AccessToken> q = em.createQuery(
                "SELECT a FROM AccessToken a WHERE a.owner = :owner AND a.client = :client and a.expireTime > CURRENT_TIMESTAMP",
                AccessToken.class);
        q.setParameter("owner", owner);
        q.setParameter("client", client);
        List<AccessToken> accessTokens = q.getResultList();

        for (AccessToken accessToken : accessTokens) {
            accessToken.setRevoked(true);
        }
    }
}
