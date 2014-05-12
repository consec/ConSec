package org.consec.oauth2.authzserver.utils;

import org.codehaus.jettison.json.JSONException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.consec.oauth2.authzserver.Utils;
import org.consec.oauth2.authzserver.jpa.entities.*;
import org.consec.oauth2.authzserver.jpa.enums.AuthorizedGrantType;
import org.consec.oauth2.authzserver.jpa.enums.ClientTrustLevel;
import org.consec.oauth2.authzserver.jpa.enums.OrganizationTrustLevel;
import org.consec.oauth2.authzserver.jpa.enums.OwnerType;

import javax.persistence.EntityManager;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class TrustUtilsTest {
    private EntityManager em;

    @Before
    public void setUp() throws Exception {
        PersistenceUtils.createInstance("testPersistenceUnit");
        em = PersistenceUtils.getInstance().getEntityManager();
    }

    @After
    public void tearDown() throws Exception {
        PersistenceUtils.getInstance().closeEntityManager(em);
        PersistenceUtils.getInstance().close();
        Utils.dropTestDatabase();
    }

    @Test
    public void testRegisterClient() throws JSONException {

        TrustUtils trustUtils = new TrustUtils(em);

        // create countris
        Country country1 = new Country("A", "Country A");
        Country country2 = new Country("B", "Country B");
        persist(country1, country2);

        // create owner
        Owner owner = new Owner();
        owner.setUuid("myuser");
        owner.setOwnerType(OwnerType.USER);
        persist(owner);

        // create organization
        Organization organization = new Organization();
        organization.setName("My Organization");
        persist(organization);

        // create client
        Client client = new Client();
        client.setClientId("myclient");
        client.setName("My Client");
        client.setCallbackUri("http://localhost:8080/myclient");
        List<AuthorizedGrantType> agtList = new ArrayList<AuthorizedGrantType>();
        agtList.add(AuthorizedGrantType.AUTHORIZATION_CODE);
        agtList.add(AuthorizedGrantType.CLIENT_CREDENTIALS);
        client.setAuthorizedGrantTypes(agtList);
        client.setClientSecret("somesecret");
        client.setOrganization(organization);
        List<Country> countryList = new ArrayList<Country>();
        countryList.add(country1);
        countryList.add(country2);
        client.setCountryList(countryList);
        persist(client);

        OrganizationTrust organizationTrust;
        ClientTrust clientTrust;
        CountryTrust countryTrust1;
        CountryTrust countryTrust2;

        // organization is fully trusted
        organizationTrust = new OrganizationTrust(owner.getId(), organization.getId());
        organizationTrust.setTrustLevel(OrganizationTrustLevel.FULLY);
        persist(organizationTrust);
        assertTrue(trustUtils.isTrusted(owner, client));

        // organization is not trusted
        organizationTrust.setTrustLevel(OrganizationTrustLevel.DENIED);
        merge(organizationTrust);
        assertFalse(trustUtils.isTrusted(owner, client));

        // organization is partly trusted but the client trust is not defined
        organizationTrust.setTrustLevel(OrganizationTrustLevel.PARTLY);
        merge(organizationTrust);
        assertNull(trustUtils.isTrusted(owner, client));

        // organization is partly trusted, the client is trusted
        organizationTrust.setTrustLevel(OrganizationTrustLevel.PARTLY);
        merge(organizationTrust);
        clientTrust = new ClientTrust(owner.getId(), client.getId());
        clientTrust.setTrustLevel(ClientTrustLevel.TRUSTED);
        persist(clientTrust);
        assertTrue(trustUtils.isTrusted(owner, client));

        // organization is partly trusted, the client is not trusted
        organizationTrust.setTrustLevel(OrganizationTrustLevel.PARTLY);
        clientTrust.setTrustLevel(ClientTrustLevel.NOT_TRUSTED);
        merge(organizationTrust, clientTrust);
        assertFalse(trustUtils.isTrusted(owner, client));

        // organization is fully trusted, the client is not trusted
        organizationTrust.setTrustLevel(OrganizationTrustLevel.FULLY);
        clientTrust.setTrustLevel(ClientTrustLevel.NOT_TRUSTED);
        merge(organizationTrust, clientTrust);
        assertTrue(trustUtils.isTrusted(owner, client));

        // organization is not trusted, the client is trusted
        organizationTrust.setTrustLevel(OrganizationTrustLevel.DENIED);
        clientTrust.setTrustLevel(ClientTrustLevel.TRUSTED);
        merge(organizationTrust, clientTrust);
        assertFalse(trustUtils.isTrusted(owner, client));

        // organization is fully trusted, owner's country restriction is on, trusted countries are not defined
        owner.setCountryRestriction(true);
        organizationTrust.setTrustLevel(OrganizationTrustLevel.FULLY);
        merge(owner, organizationTrust);
        assertNull(trustUtils.isTrusted(owner, client));

        // just one country is trusted (client can run in two countries)
        countryTrust1 = new CountryTrust(owner.getId(), country1.getCode());
        countryTrust1.setIsTrusted(true);
        countryTrust2 = new CountryTrust(owner.getId(), country2.getCode());
        countryTrust2.setIsTrusted(false);
        persist(countryTrust1, countryTrust2);
        assertFalse(trustUtils.isTrusted(owner, client));

        // both countries are trusted
        countryTrust1.setIsTrusted(true);
        countryTrust2.setIsTrusted(true);
        persist(countryTrust1, countryTrust2);
        assertTrue(trustUtils.isTrusted(owner, client));
    }

    private void persist(Object... objects) {
        em.getTransaction().begin();
        for (int i=0; i<objects.length; i++) {
            em.persist(objects[i]);
        }
        em.getTransaction().commit();
    }

    private void merge(Object... objects) {
        em.getTransaction().begin();
        for (int i=0; i<objects.length; i++) {
            em.merge(objects[i]);
        }
        em.getTransaction().commit();
    }
}
