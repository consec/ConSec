/**
 *
 */
package org.ow2.contrail.federation.federationapi.resources;

import org.apache.log4j.Logger;
import org.consec.federationdb.model.Attribute;
import org.consec.federationdb.utils.EMF;
import org.ow2.contrail.federation.federationapi.utils.JSONObject;

import javax.persistence.EntityManager;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;

/**
 * @author ales
 */
@Path("/attributes/{attrUuid}")
public class AttributeResource {

    protected static Logger logger =
            Logger.getLogger(AttributeResource.class);

    public static final String PROVIDER_SUBJECT_SLASOI_ID = "urn:contrail:names:provider:subject:slasoi-id";

    private String attrUuid;

    public AttributeResource(@PathParam("attrUuid") String attrUuid) {
        this.attrUuid = attrUuid;
    }

    /* (non-Javadoc)
      * @see org.ow2.contrail.federation.federationapi.interfaces.BaseSingle#get()
      */
    @GET
    @Produces("application/json")
    public Response get() throws Exception {
        EntityManager em = EMF.createEntityManager();
        try {
            Attribute attribute = em.find(Attribute.class, attrUuid);
            if (attribute == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }

            JSONObject attr = new JSONObject();
            attr.put("uuid", attribute.getAttributeId());
            attr.put("name", attribute.getName());
            attr.put("uri", attribute.getUri());
            attr.put("defaultValue", attribute.getDefaultValue());
            attr.put("reference", attribute.getReference());
            attr.put("description", attribute.getDescription());

            return Response.ok(attr.toString()).build();
        }
        finally {
            logger.debug("Exiting remove attribute.");
            EMF.closeEntityManager(em);
        }
    }

    /* (non-Javadoc)
      * @see org.ow2.contrail.federation.federationapi.interfaces.BaseSingle#delete()
      */
    @DELETE
    public Response delete() throws Exception {
        logger.debug("Entering remove attribute.");

        EntityManager em = EMF.createEntityManager();

        try {
            Attribute attribute = em.find(Attribute.class, attrUuid);
            if (attribute == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }

            em.getTransaction().begin();
            em.remove(attribute);
            em.getTransaction().commit();

            return Response.status(Response.Status.NO_CONTENT).build();
        }
        finally {
            logger.debug("Exiting remove attribute.");
            EMF.closeEntityManager(em);
        }
    }

    /* (non-Javadoc)
      * @see org.ow2.contrail.federation.federationapi.interfaces.BaseSingle#put(java.lang.String)
      */
    @PUT
    @Consumes("application/json")
    @Produces("application/json")
    public Response put(JSONObject attrData) throws Exception {

        EntityManager em = EMF.createEntityManager();

        try {
            Attribute attribute = em.find(Attribute.class, attrUuid);
            if (attribute == null) {
                throw new WebApplicationException(Response.Status.NOT_FOUND);
            }

            em.getTransaction().begin();

            if (attrData.has("name"))
                attribute.setName(attrData.getString("name"));
            if (attrData.has("uri"))
                attribute.setUri((String) attrData.get("uri"));
            if (attrData.has("defaultValue"))
                attribute.setDefaultValue((String) attrData.get("defaultValue"));
            if (attrData.has("reference"))
                attribute.setReference((String) attrData.get("reference"));
            if (attrData.has("description"))
                attribute.setDescription((String) attrData.get("description"));

            em.getTransaction().commit();

            return Response.status(Response.Status.NO_CONTENT).build();
        }
        finally {
            EMF.closeEntityManager(em);
        }
    }
}
