package org.consec.oauth2.authzserver.adminapi;

import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.test.framework.JerseyTest;
import com.sun.jersey.test.framework.WebAppDescriptor;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.consec.oauth2.authzserver.Utils;
import org.consec.oauth2.authzserver.jpa.enums.AuthorizedGrantType;
import org.consec.oauth2.authzserver.jpa.enums.OrganizationTrustLevel;
import org.consec.oauth2.authzserver.jpa.enums.OwnerType;
import org.consec.oauth2.authzserver.utils.PersistenceUtils;

import javax.persistence.EntityManager;

import static org.junit.Assert.assertEquals;

@Ignore
public class AdminApiTest extends JerseyTest {
    private EntityManager em;

    public AdminApiTest() throws Exception {
        super(new WebAppDescriptor.Builder("org.consec.oauth2.authzserver.adminapi")
                .contextPath("oauth-as").build());
    }

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
    public void testAdminApi() throws JSONException {
        WebResource webResource = resource();
        String baseUri = webResource.getURI().toString();
        ClientResponse response;
        JSONObject data;
        String uri;

        // create owner
        JSONObject ownerData = new JSONObject();
        ownerData.put("uuid", "user1");
        ownerData.put("owner_type", OwnerType.USER);
        response = webResource.path("/owners").post(ClientResponse.class, ownerData);
        assertEquals(response.getStatus(), 201);
        String ownerUri = response.getHeaders().getFirst("Location");
        assertEquals(ownerUri, baseUri + "/owners/user1");

        // get owner
        JSONObject ownerInfo = webResource.path("/owners/user1").get(JSONObject.class);
        assertEquals(ownerInfo.getInt("id"), 1);
        assertEquals(ownerInfo.getString("uuid"), "user1");
        assertEquals(ownerInfo.getString("owner_type"), OwnerType.USER.name());
        assertEquals(ownerInfo.getBoolean("country_restriction"), false);
        int ownerId = ownerInfo.getInt("id");

        // create organization
        JSONObject organizationData = new JSONObject();
        organizationData.put("name", "My Organization");
        response = webResource.path("/organizations").post(ClientResponse.class, organizationData);
        assertEquals(response.getStatus(), 201);
        String organizationUri = response.getHeaders().getFirst("Location");
        assertEquals(organizationUri, baseUri + "/organizations/1");

        // get organization
        JSONObject organizationInfo = webResource.path("/organizations/1").get(JSONObject.class);
        assertEquals(organizationInfo.getInt("id"), 1);
        assertEquals(organizationInfo.getString("name"), "My Organization");
        int organizationId = organizationInfo.getInt("id");

        // create client
        JSONObject clientData = new JSONObject();
        clientData.put("client_id", "myclient");
        clientData.put("name", "My Client");
        clientData.put("callback_uri", "http://localhost:8080/myclient");
        JSONArray agtArr = new JSONArray();
        agtArr.put(AuthorizedGrantType.AUTHORIZATION_CODE);
        agtArr.put(AuthorizedGrantType.CLIENT_CREDENTIALS);
        clientData.put("authorized_grant_types", agtArr);
        clientData.put("client_secret", "somesecret");
        clientData.put("organization_id", organizationId);
        JSONArray countriesArr = new JSONArray();
        clientData.put("countries", countriesArr);
        response = webResource.path("/clients").post(ClientResponse.class, clientData);
        assertEquals(response.getStatus(), 201);
        String clientUri = response.getHeaders().getFirst("Location");
        assertEquals(clientUri, baseUri + "/clients/1");

        // get client
        JSONObject clientInfo = webResource.path("/clients/1").get(JSONObject.class);
        assertEquals(clientInfo.getInt("id"), 1);
        assertEquals(clientInfo.getString("client_id"), "myclient");
        int clientId = clientInfo.getInt("id");

        // add organization trust
        data = new JSONObject();
        data.put("organization_id", organizationId);
        data.put("trust_level", OrganizationTrustLevel.FULLY);
        response = webResource.path("/owners/user1/trust/organizations").post(ClientResponse.class, data);
        assertEquals(response.getStatus(), 201);
        uri = response.getHeaders().getFirst("Location");
        assertEquals(uri, baseUri + "/owners/user1/trust/organizations/1");

        // get client
        JSONObject trustInfo = webResource.path("/owners/user1/trust/organizations/1").get(JSONObject.class);
        assertEquals(trustInfo.getString("trust_level"), OrganizationTrustLevel.FULLY);
        assertEquals(trustInfo.getJSONObject("organization").getInt("id"), organizationId);
        assertEquals(trustInfo.getJSONObject("organization").getString("uri"), "/organizations/1");
    }
}
