package org.ow2.contrail.federation.federationapi.resources.impl;

import org.apache.log4j.Logger;
import org.ow2.contrail.federation.federationapi.resources.IUserhasAttributeResource;
import org.ow2.contrail.federation.federationapi.utils.FederationDBCommon;
import org.ow2.contrail.federation.federationapi.utils.JSONObject;
import org.ow2.contrail.federation.federationdb.jpa.entities.Attribute;
import org.ow2.contrail.federation.federationdb.jpa.entities.User;
import org.ow2.contrail.federation.federationdb.jpa.entities.UserhasAttribute;
import org.ow2.contrail.federation.federationdb.utils.PersistenceUtils;

import javax.persistence.EntityManager;
import javax.ws.rs.core.Response;
import java.util.ArrayList;

public class UserhasAttributeResource implements IUserhasAttributeResource {

    protected static Logger logger =
            Logger.getLogger(UserhasAttributeResource.class);

    private UserhasAttribute userattr = null;
    private User user = null;
    private Attribute attribute = null;

    public UserhasAttributeResource(User user, Attribute attribute, UserhasAttribute attr) {
        this.userattr = attr;
        this.user = user;
        this.attribute = attribute;
    }

    public Response get() throws Exception {
        JSONObject attr = null;
        attr = new JSONObject();
        attr.put("value", this.userattr.getValue());
        attr.put("referenceId", this.userattr.getReferenceId());
        attr.put("userId", String.format("/users/%d", this.user.getUserId()));
        if (this.userattr.getAttribute() != null)
            attr.put("attributeId", String.format("/attributes/%d", this.userattr.getAttribute().getAttributeId()));
        else
            logger.error("Attribute is null!");
        return Response.ok(attr.toString()).build();
    }

    public ArrayList<String> getSubresources() throws Exception {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * Remove user attribute, update lists in Attribute and User entities.
     */
    public Response delete() throws Exception {
        logger.debug("Entering delete user attributes.");
        EntityManager em = PersistenceUtils.getInstance().getEntityManager();
        try {
            em.getTransaction().begin();
            this.userattr = em.merge(this.userattr);
            em.remove(this.userattr);
            user.getUserhasAttributeList().remove(this.userattr);
            attribute.getUserhasAttributeList().remove(this.userattr);
            user = em.merge(user);
            attribute = em.merge(attribute);
            em.getTransaction().commit();
            return Response.status(Response.Status.NO_CONTENT).build();
        }
        finally {
            logger.debug("Exiting delete user attributes.");
            PersistenceUtils.getInstance().closeEntityManager(em);
        }
    }

    public Response put(String content) throws Exception {
        logger.debug("Entering put user attributes.");
        JSONObject attrData = null;
        try {
            attrData = new JSONObject(content);
        }
        catch (Exception err) {
            logger.error(err.getMessage());
            logger.error(FederationDBCommon.getStackTrace(err));
            return Response.status(Response.Status.NOT_ACCEPTABLE).build();
        }

        if (attrData.has("value"))
            this.userattr.setValue(attrData.getString("value"));
        if (attrData.has("referenceId"))
            this.userattr.setReferenceId(attrData.getInt("referenceId"));

        EntityManager em = PersistenceUtils.getInstance().getEntityManager();
        try {
            em.getTransaction().begin();
            userattr = em.merge(this.userattr);
            em.getTransaction().commit();
            return Response.status(Response.Status.NO_CONTENT).build();
        }
        finally {
            logger.debug("Entering put user attributes.");
            PersistenceUtils.getInstance().closeEntityManager(em);
        }
    }

}
