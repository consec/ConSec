package org.consec.oauth2.authzserver.adminapi;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.consec.oauth2.authzserver.jpa.dao.ClientDao;
import org.consec.oauth2.authzserver.jpa.dao.OwnerDao;
import org.consec.oauth2.authzserver.jpa.entities.*;
import org.consec.oauth2.authzserver.jpa.enums.ClientTrustLevel;
import org.consec.oauth2.authzserver.utils.PersistenceUtils;
import org.consec.oauth2.authzserver.utils.TrustUtils;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

@Path("/owners/{ownerUuid}/trust/organizations/{orgId}/clients")
public class ClientTrustResource {

    private String ownerUuid;
    private int orgId;

    @Context
    UriInfo uriInfo;

    public ClientTrustResource(@PathParam("ownerUuid") String ownerUuid, @PathParam("orgId") int orgId) {
        this.ownerUuid = ownerUuid;
        this.orgId = orgId;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public JSONArray getClientsTrust() throws JSONException {
        EntityManager em = PersistenceUtils.getInstance().getEntityManager();
        try {
            Owner owner = new OwnerDao(em).findByUuid(ownerUuid);
            if (owner == null) {
                throw new WebApplicationException(Response.Status.NOT_FOUND);
            }
            Organization organization = em.find(Organization.class, orgId);
            if (organization == null) {
                throw new WebApplicationException(Response.Status.NOT_FOUND);
            }

            TypedQuery<ClientTrust> q = em.createNamedQuery("ClientTrust.findByOwnerIdOrgId", ClientTrust.class);
            q.setParameter("ownerId", owner.getId());
            q.setParameter("orgId", orgId);
            List<ClientTrust> clientTrustList = q.getResultList();

            JSONArray clientTrustArr = new JSONArray();
            for (ClientTrust clientTrust : clientTrustList) {
                JSONObject o = new JSONObject();
                o.put("trust_level", clientTrust.getTrustLevel());

                // client
                JSONObject clientInfo = new JSONObject();
                Client client = clientTrust.getClient();
                clientInfo.put("id", client.getId());
                clientInfo.put("name", client.getName());
                clientInfo.put("uri", UriBuilder.fromResource(ClientResource.class)
                    .path("{clientId}").build(client.getOrganization().getId(), client.getId()));
                o.put("client", clientInfo);

                // uri
                URI uri = UriBuilder.fromResource(ClientTrustResource.class)
                        .path("{clientId}")
                        .build(owner.getUuid(), orgId, clientTrust.getClient().getId());
                o.put("uri", uri);

                clientTrustArr.put(o);
            }
            return clientTrustArr;
        }
        finally {
            PersistenceUtils.getInstance().closeEntityManager(em);
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response addClientTrust(JSONObject data) throws JSONException, URISyntaxException {
        EntityManager em = PersistenceUtils.getInstance().getEntityManager();
        try {
            Owner owner = new OwnerDao(em).findByUuid(ownerUuid);
            if (owner == null) {
                throw new WebApplicationException(Response.Status.NOT_FOUND);
            }
            Organization organization = em.find(Organization.class, orgId);
            if (organization == null) {
                throw new WebApplicationException(Response.Status.NOT_FOUND);
            }

            ClientTrust clientTrust;
            Client client;
            try {
                int clientId = data.getInt("client_id");
                client = new ClientDao(em).find(clientId, orgId);
                if (client == null) {
                    throw new Exception("Invalid client_id: " + clientId);
                }

                ClientTrustLevel trustLevel = ClientTrustLevel.valueOf(data.getString("trust_level"));

                clientTrust = new ClientTrust(owner.getId(), clientId);
                clientTrust.setTrustLevel(trustLevel);
            }
            catch (Exception e) {
                throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                        .entity(e.getMessage()).build());
            }

            // store data to DB
            em.getTransaction().begin();
            em.persist(clientTrust);
            em.getTransaction().commit();

            URI location = new URI(client.getId().toString());
            return Response.created(location).build();
        }
        finally {
            PersistenceUtils.getInstance().closeEntityManager(em);
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{clientId}")
    public JSONObject getClientTrust(@PathParam("clientId") int clientId) throws JSONException {
        EntityManager em = PersistenceUtils.getInstance().getEntityManager();
        try {
            Owner owner = new OwnerDao(em).findByUuid(ownerUuid);
            if (owner == null) {
                throw new WebApplicationException(Response.Status.NOT_FOUND);
            }

            ClientTrust clientTrust = em.find(ClientTrust.class,
                    new ClientTrustPK(owner.getId(), clientId));
            if (clientTrust == null) {
                throw new WebApplicationException(Response.Status.NOT_FOUND);
            }

            JSONObject o = new JSONObject();
            o.put("trust_level", clientTrust.getTrustLevel());
            o.put("owner_uuid", clientTrust.getOwner().getUuid());

            JSONObject clientInfo = new JSONObject();
            Client client = clientTrust.getClient();
            clientInfo.put("id", client.getId());
            clientInfo.put("name", client.getName());
            clientInfo.put("uri", UriBuilder.fromResource(ClientResource.class)
                    .path("{clientId}").build(client.getOrganization().getId(), client.getId()));
            clientInfo.put("organization_id", client.getOrganization().getId());
            o.put("client", clientInfo);

            return o;
        }
        finally {
            PersistenceUtils.getInstance().closeEntityManager(em);
        }
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("{clientId}")
    public Response updateClientTrust(@PathParam("clientId") int clientId, JSONObject data) throws JSONException {

        EntityManager em = PersistenceUtils.getInstance().getEntityManager();
        try {

            Owner owner = new OwnerDao(em).findByUuid(ownerUuid);
            if (owner == null) {
                throw new WebApplicationException(Response.Status.NOT_FOUND);
            }

            ClientTrust clientTrust = em.find(ClientTrust.class,
                    new ClientTrustPK(owner.getId(), clientId));
            if (clientTrust == null) {
                throw new WebApplicationException(Response.Status.NOT_FOUND);
            }

            ClientTrustLevel trustLevel = null;
            try {
                if (data.has("trust_level")) {
                    trustLevel = ClientTrustLevel.valueOf(data.getString("trust_level"));
                }
            }
            catch (Exception e) {
                throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                        .entity(e.getMessage()).build());
            }

            em.getTransaction().begin();

            clientTrust.setTrustLevel(trustLevel);
            if (trustLevel == ClientTrustLevel.NOT_TRUSTED) {
                new TrustUtils(em).revokeAccessTokens(owner, clientTrust.getClient());
            }

            em.getTransaction().commit();

            return Response.noContent().build();
        }
        finally {
            PersistenceUtils.getInstance().closeEntityManager(em);
        }
    }

    @DELETE
    @Path("{clientId}")
    public Response deleteClientTrust(@PathParam("clientId") int clientId) {

        EntityManager em = PersistenceUtils.getInstance().getEntityManager();
        try {
            Owner owner = new OwnerDao(em).findByUuid(ownerUuid);
            if (owner == null) {
                throw new WebApplicationException(Response.Status.NOT_FOUND);
            }

            ClientTrust clientTrust = em.find(ClientTrust.class,
                    new ClientTrustPK(owner.getId(), clientId));
            if (clientTrust == null) {
                throw new WebApplicationException(Response.Status.NOT_FOUND);
            }

            em.getTransaction().begin();
            em.remove(clientTrust);
            em.getTransaction().commit();

            return Response.noContent().build();
        }
        finally {
            PersistenceUtils.getInstance().closeEntityManager(em);
        }
    }

}
