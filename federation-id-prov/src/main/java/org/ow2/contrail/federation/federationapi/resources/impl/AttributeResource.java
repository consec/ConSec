/**
 *
 */
package org.ow2.contrail.federation.federationapi.resources.impl;

import org.apache.log4j.Logger;
import org.ow2.contrail.federation.federationapi.resources.IAttributeResource;
import org.ow2.contrail.federation.federationapi.utils.FederationDBCommon;
import org.ow2.contrail.federation.federationapi.utils.JSONObject;
import org.ow2.contrail.federation.federationdb.jpa.entities.Attribute;
import org.ow2.contrail.federation.federationdb.jpa.entities.User;
import org.ow2.contrail.federation.federationdb.utils.PersistenceUtils;

import javax.persistence.EntityManager;
import javax.ws.rs.core.Response;
import java.util.ArrayList;

/**
 * @author ales
 */
public class AttributeResource implements IAttributeResource {

    protected static Logger logger =
            Logger.getLogger(AttributeResource.class);

    public static final String PROVIDER_SUBJECT_SLASOI_ID = "urn:contrail:names:provider:subject:slasoi-id";

    private User user = null;
    private Attribute attribute = null;

    public AttributeResource(User user, Attribute attribute) {
        this.user = user;
        this.attribute = attribute;
    }

    public AttributeResource(Attribute attribute) {
        this.attribute = attribute;
    }

    /* (non-Javadoc)
     * @see org.ow2.contrail.federation.federationapi.interfaces.BaseSingle#getSubresources()
     */
    public ArrayList<String> getSubresources() throws Exception {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see org.ow2.contrail.federation.federationapi.interfaces.BaseSingle#get()
     */
    public Response get() throws Exception {
        JSONObject attr = null;
        attr = new JSONObject();
        attr.put("attributeId", this.attribute.getAttributeId());
        attr.put("name", this.attribute.getName());
        attr.put("uri", this.attribute.getUri());
        attr.put("defaultValue", this.attribute.getDefaultValue());
        attr.put("reference", this.attribute.getReference());
        attr.put("description", this.attribute.getDescription());
        return Response.ok(attr.toString()).build();
    }

    /* (non-Javadoc)
     * @see org.ow2.contrail.federation.federationapi.interfaces.BaseSingle#delete()
     */
    public Response delete() throws Exception {
        logger.debug("Entering remove attribute.");
        EntityManager em = PersistenceUtils.getInstance().getEntityManager();
        try {
            em.getTransaction().begin();
            attribute = em.merge(attribute);
            if (user != null) {
                user.getUserhasAttributeList().remove(attribute);
                user = em.merge(user);
            }
            else {
                em.remove(attribute);
            }
            em.getTransaction().commit();
            return Response.status(Response.Status.NO_CONTENT).build();
        }
        finally {
            logger.debug("Exiting remove attribute.");
            PersistenceUtils.getInstance().closeEntityManager(em);
        }
    }

    /* (non-Javadoc)
     * @see org.ow2.contrail.federation.federationapi.interfaces.BaseSingle#put(java.lang.String)
     */
    public Response put(String content) throws Exception {
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
        if (attrData.has("name"))
            attribute.setName(name);
        if (attrData.has("uri"))
            attribute.setUri((String) attrData.get("uri"));
        if (attrData.has("defaultValue"))
            attribute.setDefaultValue((String) attrData.get("defaultValue"));
        if (attrData.has("reference"))
            attribute.setReference((String) attrData.get("reference"));
        if (attrData.has("description"))
            attribute.setDescription((String) attrData.get("description"));

        EntityManager em = PersistenceUtils.getInstance().getEntityManager();
        try {
            em.getTransaction().begin();
            attribute = em.merge(attribute);
            em.getTransaction().commit();
            return Response.status(Response.Status.NO_CONTENT).build();
        }
        finally {
            PersistenceUtils.getInstance().closeEntityManager(em);
        }
    }

}
