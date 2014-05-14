/**
 *
 */
package org.ow2.contrail.federation.federationapi.resources.impl;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.ow2.contrail.federation.federationapi.interfaces.BaseSingle;
import org.ow2.contrail.federation.federationapi.resources.IAttributesResource;
import org.ow2.contrail.federation.federationapi.utils.FederationDBCommon;
import org.ow2.contrail.federation.federationdb.jpa.dao.AttributeDAO;
import org.ow2.contrail.federation.federationdb.jpa.entities.Attribute;
import org.ow2.contrail.federation.federationdb.utils.PersistenceUtils;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.ws.rs.Path;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.List;

/**
 * @author ales
 */
@Path("/attributes")
public class AttributesResource implements IAttributesResource {

    protected static Logger logger =
            Logger.getLogger(AttributesResource.class);

    /* (non-Javadoc)
     * @see org.ow2.contrail.federation.federationapi.interfaces.BaseCollection#get()
     */
    public Response get() throws Exception {
        EntityManager em = PersistenceUtils.getInstance().getEntityManager();
        try {
            Query query = em.createNamedQuery("Attribute.findAll");
            List<Attribute> attrList = query.getResultList();
            JSONArray UriList = new JSONArray();
            for (Attribute attr : attrList) {
                String uri = String.format("/attributes/%d", attr.getAttributeId());
                JSONObject o = new JSONObject();
                o.put("name", attr.getName());
                o.put("uri", uri);
                UriList.put(o);
            }
            return Response.ok(UriList.toString()).build();
        }
        finally {
            PersistenceUtils.getInstance().closeEntityManager(em);
        }

    }

    /* (non-Javadoc)
     * @see org.ow2.contrail.federation.federationapi.interfaces.BaseCollection#post(java.lang.String)
     */
    public Response post(String content) throws Exception {
        JSONObject attrData = null;
        try {
            attrData = new JSONObject(content);
        }
        catch (Exception err) {
            logger.error(err.getMessage());
            logger.error(FederationDBCommon.getStackTrace(err));
            return Response.status(Response.Status.NOT_ACCEPTABLE).build();
        }
        String name = (String) attrData.get("name");
        EntityManager em = PersistenceUtils.getInstance().getEntityManager();
        try {
            Query query = em.createQuery("SELECT COUNT(u) FROM Attribute u WHERE u.name=:name");
            query.setParameter("name", name);
            if ((Long) query.getSingleResult() > 0) {
                // resource is already registered
                return Response.status(Response.Status.CONFLICT).build();
            }

            Attribute attribute = new Attribute();
            if (attrData.has("name"))
                attribute.setName(name);
            else {
                logger.error("Attribue does not have a name attribute.");
                return Response.status(Response.Status.NOT_ACCEPTABLE).build();
            }

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

            URI resourceUri = new URI(String.format("/%d", attribute.getAttributeId()));
            return Response.created(resourceUri).build();
        }
        finally {
            PersistenceUtils.getInstance().closeEntityManager(em);
        }
    }

    /* (non-Javadoc)
     * @see org.ow2.contrail.federation.federationapi.interfaces.BaseCollection#subResource(java.lang.String)
     */
    public BaseSingle subResource(int id) throws Exception {
        Attribute attr = AttributeDAO.findById(id);
        if (attr == null) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
        else {
            return new AttributeResource(attr);
        }
    }

}
