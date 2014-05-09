/**
 *
 */
package org.ow2.contrail.federation.federationapi.resources;

import org.apache.log4j.Logger;
import org.consec.federationdb.model.UserHasIdentityProvider;
import org.consec.federationdb.model.UserHasIdentityProviderPK;
import org.consec.federationdb.utils.EMF;
import org.ow2.contrail.federation.federationapi.utils.JSONObject;

import javax.persistence.EntityManager;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;

/**
 * @author ales
 */
@Path("/users/{userUuid}/ids/{idpUuid}")
public class UserIdentityProviderResource {

    protected static Logger logger =
            Logger.getLogger(UserIdentityProviderResource.class);

    private String userUuid;
    private String idpUuid;

    public UserIdentityProviderResource(@PathParam("userUuid") String userUuid, @PathParam("idpUuid") String idpUuid) {
        this.userUuid = userUuid;
        this.idpUuid = idpUuid;
    }

    @GET
    @Produces("application/json")
    public Response get() throws Exception {

        EntityManager em = EMF.createEntityManager();

        try {
            UserHasIdentityProvider userIdp = em.find(UserHasIdentityProvider.class,
                    new UserHasIdentityProviderPK(userUuid, idpUuid));
            if (userIdp == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }

            JSONObject json = new JSONObject();
            json.put("userId", String.format("/users/%s", userIdp.getUser().getUserId()));
            json.put("identityProviderId", String.format("/idps/%s", userIdp.getIdentityProvider().getIdpId()));
            json.put("identity", userIdp.getIdentity());
            json.put("attributes", userIdp.getAttributes());
            return Response.ok(json.toString()).build();
        }
        finally {
            EMF.closeEntityManager(em);
        }
    }

    @DELETE
    public Response delete() throws Exception {

        EntityManager em = EMF.createEntityManager();

        try {
            UserHasIdentityProvider userIdp = em.find(UserHasIdentityProvider.class,
                    new UserHasIdentityProviderPK(userUuid, idpUuid));
            if (userIdp == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }

            em.getTransaction().begin();
            userIdp.getUser().getUserHasIdentityProviderList().remove(userIdp);
            userIdp.getIdentityProvider().getUserHasIdentityProviderList().remove(userIdp);
            em.remove(userIdp);
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
    public Response put(JSONObject idData) throws Exception {

        EntityManager em = EMF.createEntityManager();

        try {
            UserHasIdentityProvider uidp = em.find(UserHasIdentityProvider.class,
                    new UserHasIdentityProviderPK(userUuid, idpUuid));
            if (uidp == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }

            em.getTransaction().begin();

            if (idData.has("identity"))
                uidp.setIdentity(idData.getString("identity"));
            if (idData.has("attributes"))
                uidp.setAttributes(idData.getString("attributes"));

            em.getTransaction().commit();

            return Response.status(Response.Status.NO_CONTENT).build();
        }
        catch (Exception err) {
            logger.error(err.getMessage());
            return Response.serverError().build();
        }
        finally {
            EMF.closeEntityManager(em);
        }
    }
}
