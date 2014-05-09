/**
 *
 */
package org.ow2.contrail.federation.federationapi.resources;

import org.apache.log4j.Logger;
import org.consec.federationdb.model.IdentityProvider;
import org.consec.federationdb.model.UserHasIdentityProvider;
import org.consec.federationdb.utils.EMF;
import org.json.JSONArray;
import org.ow2.contrail.federation.federationapi.utils.JSONObject;
import org.ow2.contrail.federation.federationapi.utils.RestUriBuilder;

import javax.persistence.EntityManager;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;

/**
 * @author ales
 */
@Path("/idps/{idpUuid}")
public class IdentityProviderResource {

    protected static Logger logger = Logger.getLogger(IdentityProviderResource.class);
    private String idpUuid;

    public IdentityProviderResource(@PathParam("idpUuid") String idpUuid) {
        this.idpUuid = idpUuid;
    }

    @GET
    @Produces("application/json")
    public Response get() throws Exception {
        logger.debug("Entering get");

        EntityManager em = EMF.createEntityManager();
        try {
            IdentityProvider idp = em.find(IdentityProvider.class, idpUuid);
            if (idp == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }

            String baseUri = RestUriBuilder.getIdpUri(idp);
            JSONObject json = new JSONObject();
            json.put("uuid", idp.getIdpId());
            json.put("providerURI", idp.getUri());
            json.put("description", idp.getDescription());
            json.put("providerName", idp.getName());
            json.put("users", baseUri + "/users");
            json.put("attributes", baseUri + "/attributes");
            logger.debug("Exiting get");
            return Response.ok(json.toString()).build();
        }
        finally {
            EMF.closeEntityManager(em);
        }
    }

    /**
     * @return the list of users who are in this group.
     * @throws Exception
     */
    @GET
    @Produces("application/json")
    @Path("/users")
    public Response getUsers() throws Exception {
        EntityManager em = EMF.createEntityManager();
        try {
            IdentityProvider idp = em.find(IdentityProvider.class, idpUuid);
            if (idp == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }

            JSONArray attr = new JSONArray();
            for (UserHasIdentityProvider user : idp.getUserHasIdentityProviderList()) {
                JSONObject o = new JSONObject();
                o.put("name", user.getUser().getUsername());
                o.put("uri", RestUriBuilder.getUserUri(user.getUser()));
                attr.put(o);
            }
            return Response.ok(attr.toString()).build();
        }
        finally {
            EMF.closeEntityManager(em);
        }
    }

    @DELETE
    public Response delete() throws Exception {
        EntityManager em = EMF.createEntityManager();
        try {
            IdentityProvider idp = em.find(IdentityProvider.class, idpUuid);
            if (idp == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }

            em.getTransaction().begin();
            em.remove(idp);
            em.getTransaction().commit();

            return Response.status(Response.Status.NO_CONTENT).build();
        }
        finally {
            EMF.closeEntityManager(em);
        }
    }

    @PUT
    @Consumes("application/json")
    @Produces("application/json")
    public Response put(JSONObject idpData) throws Exception {
        logger.debug("Entering put");

        EntityManager em = EMF.createEntityManager();

        try {
            IdentityProvider idp = em.find(IdentityProvider.class, idpUuid);
            if (idp == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }

            logger.debug("Commiting new Idp");
            em.getTransaction().begin();

            if (idpData.has("description"))
                idp.setDescription(idpData.getString("description"));
            if (idpData.has("providerURI"))
                idp.setUri(idpData.getString("providerURI"));
            if (idpData.has("providerName"))
                idp.setName(idpData.getString("providerName"));

            em.getTransaction().commit();

            return Response.status(Response.Status.NO_CONTENT).build();
        }
        finally {
            EMF.closeEntityManager(em);
            logger.debug("Exiting put");
        }
    }
}
