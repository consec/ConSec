package org.consec.oauth2.authzserver.adminapi;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.consec.oauth2.authzserver.jpa.dao.OwnerDao;
import org.consec.oauth2.authzserver.jpa.entities.*;
import org.consec.oauth2.authzserver.jpa.enums.ClientTrustLevel;
import org.consec.oauth2.authzserver.jpa.enums.OrganizationTrustLevel;
import org.consec.oauth2.authzserver.utils.PersistenceUtils;
import org.consec.oauth2.authzserver.utils.TrustUtils;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

@Path("/owners/{ownerUuid}/trust/organizations")
public class OrganizationTrustResource {

    private String ownerUuid;

    @Context
    UriInfo uriInfo;

    public OrganizationTrustResource(@PathParam("ownerUuid") String ownerUuid) {
        this.ownerUuid = ownerUuid;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public JSONArray getOrganizationsTrust() throws JSONException {
        EntityManager em = PersistenceUtils.getInstance().getEntityManager();
        try {
            Owner owner = new OwnerDao(em).findByUuid(ownerUuid);
            if (owner == null) {
                throw new WebApplicationException(Response.Status.NOT_FOUND);
            }

            TypedQuery<OrganizationTrust> q = em.createNamedQuery("OrganizationTrust.findByOwnerId", OrganizationTrust.class);
            q.setParameter("ownerId", owner.getId());
            List<OrganizationTrust> organizationTrustList = q.getResultList();

            JSONArray orgTrustArr = new JSONArray();
            for (OrganizationTrust orgTrust : organizationTrustList) {
                JSONObject o = new JSONObject();
                o.put("trust_level", orgTrust.getTrustLevel());

                // organization
                JSONObject orgInfo = new JSONObject();
                orgInfo.put("id", orgTrust.getOrganization().getId());
                orgInfo.put("name", orgTrust.getOrganization().getName());
                orgInfo.put("uri", UriBuilder.fromResource(OrganizationResource.class)
                        .path(Integer.toString(orgTrust.getOrganization().getId())).build());
                o.put("organization", orgInfo);

                // uri
                URI uri = UriBuilder.fromResource(OrganizationTrustResource.class)
                        .path("{organizationId}")
                        .build(owner.getUuid(), orgTrust.getOrganization().getId());
                o.put("uri", uri);

                orgTrustArr.put(o);
            }
            return orgTrustArr;
        }
        finally {
            PersistenceUtils.getInstance().closeEntityManager(em);
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response addOrganizationTrust(JSONObject data) throws JSONException, URISyntaxException {
        EntityManager em = PersistenceUtils.getInstance().getEntityManager();
        try {
            Owner owner = new OwnerDao(em).findByUuid(ownerUuid);
            if (owner == null) {
                throw new WebApplicationException(Response.Status.NOT_FOUND);
            }

            OrganizationTrust organizationTrust;
            Organization organization;
            try {
                int organizationId = data.getInt("organization_id");
                organization = em.find(Organization.class, organizationId);
                if (organization == null) {
                    throw new Exception("Invalid organization_id: " + organizationId);
                }

                OrganizationTrustLevel trustLevel = OrganizationTrustLevel.valueOf(data.getString("trust_level"));

                organizationTrust = new OrganizationTrust(owner.getId(), organizationId);
                organizationTrust.setTrustLevel(trustLevel);
            }
            catch (Exception e) {
                throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                        .entity(e.getMessage()).build());
            }

            // store data to DB
            em.getTransaction().begin();
            em.persist(organizationTrust);
            em.getTransaction().commit();

            URI location = new URI(organization.getId().toString());
            return Response.created(location).build();
        }
        finally {
            PersistenceUtils.getInstance().closeEntityManager(em);
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{organizationId}")
    public JSONObject getOrganizationTrust(@PathParam("organizationId") int organizationId) throws JSONException {
        EntityManager em = PersistenceUtils.getInstance().getEntityManager();
        try {
            Owner owner = new OwnerDao(em).findByUuid(ownerUuid);
            if (owner == null) {
                throw new WebApplicationException(Response.Status.NOT_FOUND);
            }

            OrganizationTrust organizationTrust = em.find(OrganizationTrust.class,
                    new OrganizationTrustPK(owner.getId(), organizationId));
            if (organizationTrust == null) {
                throw new WebApplicationException(Response.Status.NOT_FOUND);
            }

            JSONObject o = new JSONObject();
            o.put("trust_level", organizationTrust.getTrustLevel());
            o.put("owner_uuid", organizationTrust.getOwner().getUuid());

            JSONObject organizationInfo = new JSONObject();
            organizationInfo.put("id", organizationTrust.getOrganization().getId());
            organizationInfo.put("name", organizationTrust.getOrganization().getName());
            organizationInfo.put("uri", UriBuilder.fromResource(OrganizationResource.class)
                    .path(Integer.toString(organizationTrust.getOrganization().getId())).build());
            o.put("organization", organizationInfo);

            return o;
        }
        finally {
            PersistenceUtils.getInstance().closeEntityManager(em);
        }
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("{organizationId}")
    public Response updateOrganizationTrust(@PathParam("organizationId") int organizationId, JSONObject data) throws JSONException {

        EntityManager em = PersistenceUtils.getInstance().getEntityManager();
        try {

            Owner owner = new OwnerDao(em).findByUuid(ownerUuid);
            if (owner == null) {
                throw new WebApplicationException(Response.Status.NOT_FOUND);
            }

            OrganizationTrust organizationTrust = em.find(OrganizationTrust.class,
                    new OrganizationTrustPK(owner.getId(), organizationId));
            if (organizationTrust == null) {
                throw new WebApplicationException(Response.Status.NOT_FOUND);
            }
            Organization organization = organizationTrust.getOrganization();

            OrganizationTrustLevel trustLevel = null;
            try {
                if (data.has("trust_level")) {
                    trustLevel = OrganizationTrustLevel.valueOf(data.getString("trust_level"));
                }
            }
            catch (Exception e) {
                throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                        .entity(e.getMessage()).build());
            }

            em.getTransaction().begin();

            organizationTrust.setTrustLevel(trustLevel);
            if (trustLevel == OrganizationTrustLevel.DENIED) {
                for (Client client : organization.getClientList()) {
                    new TrustUtils(em).revokeAccessTokens(owner, client);
                }
            }
            if (trustLevel == OrganizationTrustLevel.PARTLY) {
                for (Client client : organization.getClientList()) {
                    ClientTrust clientTrust = new ClientTrust(owner.getId(), client.getId());
                    if (clientTrust.getTrustLevel() == ClientTrustLevel.NOT_TRUSTED)
                        new TrustUtils(em).revokeAccessTokens(owner, client);
                }
            }

            em.getTransaction().commit();

            return Response.noContent().build();
        }
        finally {
            PersistenceUtils.getInstance().closeEntityManager(em);
        }
    }

    @DELETE
    @Path("{organizationId}")
    public Response deleteOrganizationTrust(@PathParam("organizationId") int organizationId) {

        EntityManager em = PersistenceUtils.getInstance().getEntityManager();
        try {
            Owner owner = new OwnerDao(em).findByUuid(ownerUuid);
            if (owner == null) {
                throw new WebApplicationException(Response.Status.NOT_FOUND);
            }

            OrganizationTrust organizationTrust = em.find(OrganizationTrust.class,
                    new OrganizationTrustPK(owner.getId(), organizationId));
            if (organizationTrust == null) {
                throw new WebApplicationException(Response.Status.NOT_FOUND);
            }

            em.getTransaction().begin();
            em.remove(organizationTrust);
            em.getTransaction().commit();

            return Response.noContent().build();
        }
        finally {
            PersistenceUtils.getInstance().closeEntityManager(em);
        }
    }

}
