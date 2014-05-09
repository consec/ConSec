package org.ow2.contrail.federation.federationapi.resources;

import org.apache.log4j.Logger;
import org.consec.federationdb.model.User;
import org.consec.federationdb.model.UserHasAttribute;
import org.consec.federationdb.model.UserHasAttributePK;
import org.consec.federationdb.utils.EMF;
import org.ow2.contrail.federation.federationapi.utils.JSONObject;

import javax.persistence.EntityManager;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;

@Path("/users/{userUuid}/attributes/{attrUuid}")
public class UserAttributeResource {

    protected static Logger logger =
            Logger.getLogger(UserAttributeResource.class);

    private String userUuid;
    private String attrUuid;

    public UserAttributeResource(@PathParam("userUuid") String userUuid, @PathParam("attrUuid") String attrUuid) {
        this.userUuid = userUuid;
        this.attrUuid = attrUuid;
    }

    @GET
    @Produces("application/json")
    public Response get() throws Exception {

        EntityManager em = EMF.createEntityManager();

        try {
            User user = em.find(User.class, userUuid);
            UserHasAttribute userAttr = em.find(UserHasAttribute.class, new UserHasAttributePK(userUuid, attrUuid));
            if (userAttr == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }

            JSONObject json = new JSONObject();
            json.put("value", userAttr.getValue());
            json.put("referenceId", userAttr.getReferenceId());
            json.put("userId", String.format("/users/%s", user.getUserId()));
            if (userAttr.getAttribute() != null)
                json.put("attributeUuid", userAttr.getAttribute().getAttributeId());
            else
                logger.error("Attribute is null!");
            return Response.ok(json.toString()).build();
        }
        finally {
            EMF.closeEntityManager(em);
        }
    }

    /**
     * Remove user attribute, update lists in Attribute and User entities.
     */
    @DELETE
    public Response delete() throws Exception {
        logger.debug("Entering delete user attributes.");

        EntityManager em = EMF.createEntityManager();

        try {
            UserHasAttribute userAttr = em.find(UserHasAttribute.class, new UserHasAttributePK(userUuid, attrUuid));
            if (userAttr == null) {
                throw new WebApplicationException(Response.Status.NOT_FOUND);
            }

            em.getTransaction().begin();
            em.remove(userAttr);
            //user.getUserhasAttributeList().remove(userattr);
            //attribute.getUserhasAttributeList().remove(this.userattr);
            em.getTransaction().commit();
            return Response.status(Response.Status.NO_CONTENT).build();
        }
        finally {
            logger.debug("Exiting delete user attributes.");
            EMF.closeEntityManager(em);
        }
    }

    @PUT
    @Consumes("application/json")
    @Produces("application/json")
    public Response put(JSONObject attrData) throws Exception {
        logger.debug("Entering put user attributes.");

        EntityManager em = EMF.createEntityManager();
        try {
            UserHasAttribute userAttr = em.find(UserHasAttribute.class, new UserHasAttributePK(userUuid, attrUuid));
            if (userAttr == null) {
                throw new WebApplicationException(Response.Status.NOT_FOUND);
            }

            em.getTransaction().begin();

            if (attrData.has("value"))
                userAttr.setValue(attrData.getString("value"));
            if (attrData.has("referenceId"))
                userAttr.setReferenceId(attrData.getInt("referenceId"));

            em.getTransaction().commit();

            return Response.status(Response.Status.NO_CONTENT).build();
        }
        finally {
            logger.debug("Entering put user attributes.");
            EMF.closeEntityManager(em);
        }
    }
}
