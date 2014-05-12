package org.consec.oauth2.authzserver.adminapi;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.consec.oauth2.authzserver.jpa.dao.AccessTokenDao;
import org.consec.oauth2.authzserver.jpa.entities.*;
import org.consec.oauth2.authzserver.utils.PersistenceUtils;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.util.List;

@Path("/access_tokens/{token}")
public class AccessTokenResource {

    private String ownerUuid;
    private String token;

    @Context
    UriInfo uriInfo;

    public AccessTokenResource(@PathParam("ownerUuid") String ownerUuid, @PathParam("token") String token) {
        this.ownerUuid = ownerUuid;
        this.token = token;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public JSONObject getAccessTokenInfo() throws JSONException {
        EntityManager em = PersistenceUtils.getInstance().getEntityManager();
        try {
            AccessToken accessToken = new AccessTokenDao(em).findByToken(token);
            if (accessToken == null) {
                throw new WebApplicationException(Response.Status.NOT_FOUND);
            }

            JSONObject json = new JSONObject();
            json.put("access_token", accessToken.getToken());
            json.put("expire_time", accessToken.getExpireTime());
            json.put("owner", accessToken.getOwner().getUuid());

            Owner owner = accessToken.getOwner();
            JSONObject ownerJson = new JSONObject();
            ownerJson.put("uuid", owner.getUuid());
            json.put("owner", ownerJson);

            Client client = accessToken.getClient();
            JSONObject clientJson = new JSONObject();
            clientJson.put("name", client.getName());
            clientJson.put("id", client.getId());
            clientJson.put("client_id", client.getClientId());
            JSONArray countriesJson = new JSONArray();
            for (Country country : client.getCountryList()) {
                JSONObject countryJson = new JSONObject();
                countryJson.put("code", country.getCode());
                countryJson.put("name", country.getName());
                countriesJson.put(countryJson);
            }
            clientJson.put("countries", countriesJson);
            Organization organization = client.getOrganization();
            JSONObject orgJson = new JSONObject();
            orgJson.put("id", organization.getId());
            orgJson.put("name", organization.getName());
            clientJson.put("organization", orgJson);
            json.put("client", clientJson);

            json.put("access_log",
                    UriBuilder.fromResource(AccessTokenResource.class).path("access_log").build(token));
            return json;
        }
        finally {
            PersistenceUtils.getInstance().closeEntityManager(em);
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("access_log")
    public JSONArray getAccessLog() throws JSONException {
        EntityManager em = PersistenceUtils.getInstance().getEntityManager();
        try {
            AccessToken accessToken = new AccessTokenDao(em).findByToken(token);
            if (accessToken == null) {
                throw new WebApplicationException(Response.Status.NOT_FOUND);
            }

            TypedQuery<TokenInfoAccessLog> q = em.createQuery(
                    "SELECT l FROM TokenInfoAccessLog l WHERE l.accessToken = :accessToken", TokenInfoAccessLog.class);
            q.setParameter("accessToken", accessToken);
            List<TokenInfoAccessLog> accessLogList = q.getResultList();

            JSONArray tokensArr = new JSONArray();
            for (TokenInfoAccessLog accessLog : accessLogList) {
                tokensArr.put(accessLog.toJson());
            }
            return tokensArr;
        }
        finally {
            PersistenceUtils.getInstance().closeEntityManager(em);
        }
    }
}
