package org.consec.oauth2.authzserver.adminapi;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.consec.oauth2.authzserver.jpa.dao.OwnerDao;
import org.consec.oauth2.authzserver.jpa.entities.AccessToken;
import org.consec.oauth2.authzserver.jpa.entities.Owner;
import org.consec.oauth2.authzserver.jpa.entities.TokenInfoAccessLog;
import org.consec.oauth2.authzserver.jpa.enums.OwnerType;
import org.consec.oauth2.authzserver.utils.PersistenceUtils;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

@Path("/owners")
public class OwnerResource {

    @Context
    UriInfo uriInfo;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public JSONArray getOwners() throws JSONException {
        EntityManager em = PersistenceUtils.getInstance().getEntityManager();
        try {
            TypedQuery<Owner> q = em.createNamedQuery("Owner.findAll", Owner.class);
            List<Owner> ownerList = q.getResultList();

            JSONArray ownersArr = new JSONArray();
            for (Owner owner : ownerList) {
                JSONObject o = new JSONObject();
                o.put("id", owner.getId());
                o.put("uuid", owner.getUuid());
                o.put("uri", UriBuilder.fromResource(OwnerResource.class).path("{ownerUuid}").build(owner.getUuid()));
                o.put("owner_type", owner.getOwnerType());
                ownersArr.put(o);
            }
            return ownersArr;
        }
        finally {
            PersistenceUtils.getInstance().closeEntityManager(em);
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response addOwner(JSONObject data) throws Exception {

        Owner owner;
        try {
            owner = new Owner();

            String uuid = data.getString("uuid");
            owner.setUuid(uuid);

            OwnerType ownerType = OwnerType.valueOf(data.getString("owner_type"));
            owner.setOwnerType(ownerType);

            if (data.has("country_restriction")) {
                boolean countryRestriction = data.getBoolean("country_restriction");
                owner.setCountryRestriction(countryRestriction);
            }
        }
        catch (Exception e) {
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                    .entity(e.getMessage()).build());
        }

        EntityManager em = PersistenceUtils.getInstance().getEntityManager();
        try {
            em.getTransaction().begin();
            em.persist(owner);
            em.getTransaction().commit();

            URI location = new URI(owner.getUuid());
            return Response.created(location).build();
        }
        finally {
            PersistenceUtils.getInstance().closeEntityManager(em);
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{ownerUuid}")
    public JSONObject getOwner(@PathParam("ownerUuid") String ownerUuid) throws JSONException {
        EntityManager em = PersistenceUtils.getInstance().getEntityManager();
        try {
            Owner owner = new OwnerDao(em).findByUuid(ownerUuid);
            if (owner == null) {
                throw new WebApplicationException(Response.Status.NOT_FOUND);
            }

            JSONObject ownerInfo = new JSONObject();
            ownerInfo.put("id", owner.getId());
            ownerInfo.put("uuid", owner.getUuid());
            URI uri = UriBuilder.fromResource(OwnerResource.class).path("{ownerUuid}").build(owner.getUuid());
            ownerInfo.put("uri", uri);
            ownerInfo.put("owner_type", owner.getOwnerType());
            ownerInfo.put("country_restriction", owner.getCountryRestriction());
            ownerInfo.put("organization_trust",
                    UriBuilder.fromResource(OrganizationTrustResource.class).build(owner.getUuid()));
            ownerInfo.put("country_trust",
                    UriBuilder.fromResource(CountryTrustResource.class).build(owner.getUuid()));

            return ownerInfo;
        }
        finally {
            PersistenceUtils.getInstance().closeEntityManager(em);
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{ownerUuid}/access_tokens")
    public JSONArray getAccessTokens(@PathParam("ownerUuid") String ownerUuid) throws URISyntaxException, JSONException {
        EntityManager em = PersistenceUtils.getInstance().getEntityManager();
        try {
            Owner owner = new OwnerDao(em).findByUuid(ownerUuid);
            if (owner == null) {
                throw new WebApplicationException(Response.Status.NOT_FOUND);
            }

            TypedQuery<AccessToken> q = em.createQuery("SELECT a FROM AccessToken a WHERE a.owner = :owner " +
                    " AND a.expireTime > CURRENT_TIMESTAMP AND a.revoked = FALSE ORDER BY a.expireTime ASC",
                    AccessToken.class);
            q.setParameter("owner", owner);
            List<AccessToken> tokenList = q.getResultList();

            JSONArray tokensArr = new JSONArray();
            for (AccessToken accessToken : tokenList) {
                JSONObject o = accessToken.toJson();
                o.put("uri",
                        UriBuilder.fromResource(OwnerResource.class)
                                .path("{ownerUuid}/access_tokens/{token}").build(ownerUuid, accessToken.getToken()));
                tokensArr.put(o);
            }
            return tokensArr;
        }
        finally {
            PersistenceUtils.getInstance().closeEntityManager(em);
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{ownerUuid}/access_log")
    public JSONArray getAccessLog(@PathParam("ownerUuid") String ownerUuid) throws URISyntaxException, JSONException {
        EntityManager em = PersistenceUtils.getInstance().getEntityManager();
        try {
            Owner owner = new OwnerDao(em).findByUuid(ownerUuid);
            if (owner == null) {
                throw new WebApplicationException(Response.Status.NOT_FOUND);
            }

            TypedQuery<Object[]> q = em.createQuery(
                    "SELECT l.id, t.token, l.bearerName, l.resourceServerName, l.timestamp " +
                            "FROM TokenInfoAccessLog l JOIN l.accessToken t " +
                            "WHERE t.owner = :owner", Object[].class);
            q.setParameter("owner", owner);
            List<Object[]> logRecords = q.getResultList();

            JSONArray jsonArray = new JSONArray();
            for (Object[] logRecord : logRecords) {
                JSONObject o = new JSONObject();
                o.put("id", logRecord[0]);
                o.put("access_token", logRecord[1]);
                o.put("bearer", logRecord[2]);
                o.put("resource_server", logRecord[3]);
                o.put("timestamp", logRecord[4]);
                jsonArray.put(o);
            }
            return jsonArray;
        }
        finally {
            PersistenceUtils.getInstance().closeEntityManager(em);
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{ownerUuid}/accesses_per_rs")
    public JSONObject getAccessesPerRS(@PathParam("ownerUuid") String ownerUuid) throws URISyntaxException, JSONException {
        EntityManager em = PersistenceUtils.getInstance().getEntityManager();
        try {
            Owner owner = new OwnerDao(em).findByUuid(ownerUuid);
            if (owner == null) {
                throw new WebApplicationException(Response.Status.NOT_FOUND);
            }

            TypedQuery<TokenInfoAccessLog> q = em.createQuery(
                    "SELECT l " +
                            "FROM TokenInfoAccessLog l JOIN l.accessToken t " +
                            "WHERE t.owner = :owner", TokenInfoAccessLog.class);
            q.setParameter("owner", owner);
            List<TokenInfoAccessLog> logRecords = q.getResultList();

            JSONObject result = new JSONObject();
            for (TokenInfoAccessLog logRecord : logRecords) {
                if (!result.has(logRecord.getResourceServerName())) {
                    result.put(logRecord.getResourceServerName(), new JSONArray());
                }
                JSONArray rsAccesses = result.getJSONArray(logRecord.getResourceServerName());

                JSONObject o = new JSONObject();
                o.put("bearer", logRecord.getBearerName());
                o.put("timestamp", logRecord.getTimestamp());
                rsAccesses.put(o);
            }

            return result;
        }
        finally {
            PersistenceUtils.getInstance().closeEntityManager(em);
        }
    }
}
