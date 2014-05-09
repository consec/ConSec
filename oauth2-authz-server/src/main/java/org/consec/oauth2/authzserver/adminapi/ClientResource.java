package org.consec.oauth2.authzserver.adminapi;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.consec.oauth2.authzserver.jpa.dao.ClientDao;
import org.consec.oauth2.authzserver.jpa.entities.Client;
import org.consec.oauth2.authzserver.jpa.entities.Country;
import org.consec.oauth2.authzserver.jpa.entities.Organization;
import org.consec.oauth2.authzserver.jpa.enums.AuthorizedGrantType;
import org.consec.oauth2.authzserver.utils.PersistenceUtils;

import javax.persistence.EntityManager;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

@Path("/organizations/{orgId}/clients")
public class ClientResource {
    private static Logger log = Logger.getLogger(ClientResource.class);
    private int orgId;

    @Context
    UriInfo uriInfo;

    public ClientResource(@PathParam("orgId") int orgId) {
        this.orgId = orgId;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public JSONArray getClients() throws URISyntaxException, JSONException {

        EntityManager em = PersistenceUtils.getInstance().getEntityManager();
        try {
            Organization organization = em.find(Organization.class, orgId);
            if (organization == null) {
                throw new WebApplicationException(Response.Status.NOT_FOUND);
            }

            JSONArray jsonArray = new JSONArray();
            for (Client client : organization.getClientList()) {
                JSONObject clientInfo = new JSONObject();
                clientInfo.put("uri", UriBuilder.fromResource(ClientResource.class)
                        .path("{clientId}").build(orgId, client.getId()));
                clientInfo.put("id", client.getId());
                clientInfo.put("client_id", client.getClientId());
                clientInfo.put("name", client.getName());

                jsonArray.put(clientInfo);
            }
            return jsonArray;
        }
        finally {
            PersistenceUtils.getInstance().closeEntityManager(em);
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response registerClient(JSONObject data) throws Exception {

        EntityManager em = PersistenceUtils.getInstance().getEntityManager();
        try {
            Organization organization = em.find(Organization.class, orgId);
            if (organization == null) {
                throw new WebApplicationException(Response.Status.NOT_FOUND);
            }

            Client client;
            try {
                // client_id
                String clientId = data.getString("client_id");

                // name
                String name = data.getString("name");

                // callback_uri
                String callbackUri = null;
                if (data.has("callback_uri")) {
                    callbackUri = data.getString("callback_uri");
                }

                // authorized_grant_types
                JSONArray agtArr = data.getJSONArray("authorized_grant_types");
                List<AuthorizedGrantType> agtList = new ArrayList<AuthorizedGrantType>();
                for (int i = 0; i < agtArr.length(); i++) {
                    AuthorizedGrantType agt = AuthorizedGrantType.valueOf(agtArr.getString(i));
                    agtList.add(agt);
                }

                // client_secret
                String clientSecret = data.getString("client_secret");

                // countries
                JSONArray countriesArr = data.getJSONArray("countries");
                List<Country> countryList = new ArrayList<Country>();
                for (int i = 0; i < countriesArr.length(); i++) {
                    Country country = em.find(Country.class, countriesArr.getString(i));
                    countryList.add(country);
                }

                client = new Client();
                client.setClientId(clientId);
                client.setName(name);
                client.setCallbackUri(callbackUri);
                client.setAuthorizedGrantTypes(agtList);
                client.setClientSecret(clientSecret);
                client.setOrganization(organization);
                client.setCountryList(countryList);
            }
            catch (Exception e) {
                throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                        .entity(e.getMessage()).build());
            }

            // store data to DB
            em.getTransaction().begin();
            em.persist(client);
            em.getTransaction().commit();

            URI location = new URI(client.getId().toString());
            return Response.created(location).build();
        }
        finally {
            PersistenceUtils.getInstance().closeEntityManager(em);
        }
    }

    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("{clientId}")
    public JSONObject getClient(@PathParam("clientId") int clientId) throws URISyntaxException, JSONException {

        EntityManager em = PersistenceUtils.getInstance().getEntityManager();
        try {
            Client client = new ClientDao(em).find(clientId, orgId);
            if (client == null) {
                throw new WebApplicationException(Response.Status.NOT_FOUND);
            }

            JSONObject o = new JSONObject();
            o.put("uri", UriBuilder.fromResource(ClientResource.class)
                    .path("{clientId}").build(orgId, client.getId()));
            o.put("id", client.getId());
            o.put("client_id", client.getClientId());
            o.put("name", client.getName());
            o.put("callback_uri", client.getCallbackUri());
            o.put("organization_id", client.getOrganization().getId());

            // authorized_grant_types
            JSONArray agtArr = new JSONArray();
            for (AuthorizedGrantType agt : client.getAuthorizedGrantTypes()) {
                agtArr.put(agt);
            }
            o.put("authorized_grant_types", agtArr);

            // countries
            JSONArray countriesArr = new JSONArray();
            for (Country country : client.getCountryList()) {
                countriesArr.put(country.getCode());
            }
            o.put("countries", countriesArr);

            return o;
        }
        finally {
            PersistenceUtils.getInstance().closeEntityManager(em);
        }
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("{clientId}")
    public Response updateClient(@PathParam("clientId") int clientId, JSONObject data) throws Exception {

        EntityManager em = PersistenceUtils.getInstance().getEntityManager();
        try {

            Client client = new ClientDao(em).find(clientId, orgId);
            if (client == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }

            try {
                // client_id
                if (data.has("client_id")) {
                    client.setClientId(data.getString("client_id"));
                }

                // name
                if (data.has("name")) {
                    client.setName(data.getString("name"));
                }

                // callback_uri
                if (data.has("callback_uri")) {
                    client.setCallbackUri(data.getString("callback_uri"));
                }

                // client_secret
                if (data.has("client_secret")) {
                    client.setClientSecret(data.getString("client_secret"));
                }

                // authorized_grant_types
                if (data.has("authorized_grant_types")) {
                    JSONArray agtArr = data.getJSONArray("authorized_grant_types");
                    List<AuthorizedGrantType> agtList = new ArrayList<AuthorizedGrantType>();
                    for (int i = 0; i < agtArr.length(); i++) {
                        AuthorizedGrantType agt = AuthorizedGrantType.valueOf(agtArr.getString(i));
                        agtList.add(agt);
                    }
                    client.setAuthorizedGrantTypes(agtList);
                }

                // organization_id
                if (data.has("organization_id")) {
                    int organizationId = data.getInt("organization_id");
                    Organization organization = em.find(Organization.class, organizationId);
                    if (organization == null) {
                        throw new Exception("Invalid organization_id.");
                    }
                    client.setOrganization(organization);
                }

                // countries
                List<Country> countryList = null;
                if (data.has("countries")) {
                    JSONArray countriesArr = data.getJSONArray("countries");
                    countryList = new ArrayList<Country>();
                    for (int i = 0; i < countriesArr.length(); i++) {
                        Country country = em.find(Country.class, countriesArr.getString(i));
                        if (country == null) {
                            throw new Exception("Invalid country code: " + countriesArr.getString(i));
                        }
                        countryList.add(country);
                    }
                    client.setCountryList(countryList);
                }
            }
            catch (Exception e) {
                throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                        .entity(e.getMessage()).build());
            }

            em.getTransaction().begin();
            em.merge(client);
            em.getTransaction().commit();

            return Response.noContent().build();
        }
        finally {
            PersistenceUtils.getInstance().closeEntityManager(em);
        }
    }

    @DELETE
    @Path("{clientId}")
    public Response deleteClient(@PathParam("clientId") int clientId) {

        EntityManager em = PersistenceUtils.getInstance().getEntityManager();
        try {
            Client client = new ClientDao(em).find(clientId, orgId);
            if (client == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }

            em.getTransaction().begin();
            em.remove(client);
            em.getTransaction().commit();

            return Response.noContent().build();
        }
        finally {
            PersistenceUtils.getInstance().closeEntityManager(em);
        }
    }
}
