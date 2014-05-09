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
import org.consec.oauth2.authzserver.utils.PersistenceUtils;

import static org.junit.Assert.assertEquals;

@Ignore
public class ClientResourceTest extends JerseyTest {

    public ClientResourceTest() throws Exception {
        super(new WebAppDescriptor.Builder("org.consec.oauth2.authzserver.adminapi")
                .contextPath("oauth-as").build());
    }

    @Before
    public void setUp() throws Exception {
        PersistenceUtils.createInstance("testPersistenceUnit");
    }

    @After
    public void tearDown() throws Exception {
       PersistenceUtils.getInstance().close();
       Utils.dropTestDatabase();
    }

    @Test
    public void testRegisterClient() throws JSONException {
        WebResource webResource = resource();
        String baseUri = webResource.getURI().toString();

        // register organization
        JSONObject orgData = new JSONObject();
        orgData.put("name", "My Organization");
        ClientResponse addOrgResponse = webResource.path("/organizations").post(ClientResponse.class, orgData);
        assertEquals(addOrgResponse.getStatus(), 201);
        String organizationUri = addOrgResponse.getHeaders().getFirst("Location");
        assertEquals(organizationUri, baseUri + "/organizations/1");

        // register client
        JSONObject clientData = new JSONObject();
        clientData.put("client_id", "myclient");
        clientData.put("name", "My Client");
        clientData.put("callback_uri", "http://localhost:8080/myclient");
        JSONArray agtArr = new JSONArray();
        agtArr.put(AuthorizedGrantType.AUTHORIZATION_CODE);
        agtArr.put(AuthorizedGrantType.CLIENT_CREDENTIALS);
        clientData.put("authorized_grant_types", agtArr);
        clientData.put("client_secret", "secret");
        clientData.put("organization_id", 1);
        JSONArray countriesArr = new JSONArray();
        clientData.put("countries", countriesArr);
        ClientResponse addClientResponse = webResource.path("/organizations/1/clients").post(ClientResponse.class, clientData);
        assertEquals(addClientResponse.getStatus(), 201);
        String clientUri = addClientResponse.getHeaders().getFirst("Location");
        assertEquals(clientUri, baseUri + "/organizations/1/clients/1");

        // get client
        JSONObject clientInfo = webResource.path("/organizations/1/clients/1").get(JSONObject.class);
        assertEquals(clientInfo.getString("client_id"), "myclient");
        JSONArray agtArr1 = clientInfo.getJSONArray("authorized_grant_types");
        assertEquals(agtArr1.getString(0), AuthorizedGrantType.AUTHORIZATION_CODE.name());
        assertEquals(agtArr1.getString(1), AuthorizedGrantType.CLIENT_CREDENTIALS.name());

        // get all clients
        JSONArray clientsArray = webResource.path("/organizations/1/clients").get(JSONArray.class);
        assertEquals(clientsArray.length(), 1);
        JSONObject clientInfo1 = clientsArray.getJSONObject(0);
        assertEquals(clientInfo1.getString("client_id"), "myclient");

        // delete client
        webResource.path("/organizations/1/clients/1").delete();

        // get all clients
        clientsArray = webResource.path("/organizations/1/clients").get(JSONArray.class);
        assertEquals(clientsArray.length(), 0);
    }

}
