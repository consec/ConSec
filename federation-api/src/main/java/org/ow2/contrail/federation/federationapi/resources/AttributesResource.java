/**
 *
 */
package org.ow2.contrail.federation.federationapi.resources;

import org.apache.log4j.Logger;
import org.consec.federationdb.model.Attribute;
import org.consec.federationdb.utils.EMF;
import org.json.JSONArray;
import org.ow2.contrail.federation.federationapi.utils.DBUtils;
import org.ow2.contrail.federation.federationapi.utils.JSONObject;
import org.ow2.contrail.federation.federationapi.utils.RestUriBuilder;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.RollbackException;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.List;
import java.util.UUID;

/**
 * @author ales
 */
@Path("/attributes")
public class AttributesResource {

    protected static Logger logger =
            Logger.getLogger(AttributesResource.class);

    /* (non-Javadoc)
      * @see org.ow2.contrail.federation.federationapi.interfaces.BaseCollection#get()
      */
    @GET
    @Produces("application/json")
    public Response getAttributes() throws Exception {
        EntityManager em = EMF.createEntityManager();
        try {
            Query query = em.createNamedQuery("Attribute.findAll");
            List<Attribute> attrList = query.getResultList();
            JSONArray uriList = new JSONArray();
            for (Attribute attr : attrList) {
                JSONObject o = new JSONObject();
                o.put("name", attr.getName());
                o.put("uri", RestUriBuilder.getAttributeUri(attr));
                uriList.put(o);
            }
            return Response.ok(uriList.toString()).build();
        }
        finally {
            EMF.closeEntityManager(em);
        }

    }

    /* (non-Javadoc)
      * @see org.ow2.contrail.federation.federationapi.interfaces.BaseCollection#post(java.lang.String)
      */
    @POST
    @Consumes("application/json")
    @Produces("application/json")
    public Response post(JSONObject attrData) throws Exception {

        EntityManager em = EMF.createEntityManager();
        try {

            Attribute attribute = new Attribute();
            if (attrData.has("name"))
                attribute.setName(attrData.getString("name"));
            else {
                logger.error("Attribue does not have a name attribute.");
                return Response.status(Response.Status.BAD_REQUEST).entity("Missing name attribute.").build();
            }

            String attrUuid = attrData.has("uuid") ? attrData.getString("uuid") : UUID.randomUUID().toString();
            attribute.setAttributeId(attrUuid);

            if (attrData.has("uri"))
                attribute.setUri((String) attrData.get("uri"));
            if (attrData.has("defaultValue"))
                attribute.setDefaultValue((String) attrData.get("defaultValue"));
            if (attrData.has("reference"))
                attribute.setReference((String) attrData.get("reference"));
            if (attrData.has("description"))
                attribute.setDescription((String) attrData.get("description"));

            em.getTransaction().begin();
            em.persist(attribute);
            em.getTransaction().commit();

            URI resourceUri = new URI(String.format("/%s", attribute.getAttributeId()));
            return Response.created(resourceUri).build();
        }
        catch (RollbackException e) {
            if (DBUtils.isIntegrityConstraintException(e)) {
                return Response.status(Response.Status.CONFLICT).build();
            }
            else {
                throw e;
            }
        }
        finally {
            EMF.closeEntityManager(em);
        }
    }
}
