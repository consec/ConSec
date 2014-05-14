/**
 *
 */
package org.ow2.contrail.federation.federationapi.resources.impl;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.ow2.contrail.federation.federationapi.resources.IUGroupResource;
import org.ow2.contrail.federation.federationapi.utils.FederationDBCommon;
import org.ow2.contrail.federation.federationapi.utils.JSONObject;
import org.ow2.contrail.federation.federationdb.jpa.entities.UGroup;
import org.ow2.contrail.federation.federationdb.jpa.entities.User;
import org.ow2.contrail.federation.federationdb.utils.PersistenceUtils;

import javax.persistence.EntityManager;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import java.util.ArrayList;

/**
 * @author ales
 */
public class UGroupResource implements IUGroupResource {

    protected static Logger logger =
            Logger.getLogger(UGroupResource.class);

    protected User user = null;
    protected UGroup group = null;

    public UGroupResource(User user, UGroup role) {
        this.user = user;
        this.group = role;
    }

    public UGroupResource(UGroup role) {
        this.user = null;
        this.group = role;
    }

    @Override
    public ArrayList<String> getSubresources() throws Exception {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Response get() throws Exception {
        JSONObject attr = null;
        attr = new JSONObject();
        attr.put("groupId", this.group.getGroupId());
        attr.put("name", this.group.getName());
        attr.put("description", this.group.getDescription());
        attr.put("users", String.format("/groups/%d/users", this.group.getGroupId()));
        return Response.ok(attr.toString()).build();
    }

    /**
     * @return the list of users who are in this group.
     * @throws Exception
     */
    @GET
    @Produces("application/json")
    @Path("/users")
    public Response getUsers() throws Exception {
        JSONArray attr = null;
        attr = new JSONArray();
        for (User user : this.group.getUserList()) {
            String uri = String.format("/users/%d", user.getUserId());
            JSONObject o = new JSONObject();
            o.put("name", group.getName());
            o.put("uri", uri);
            attr.put(o);
        }
        return Response.ok(attr.toString()).build();
    }

    @Override
    public Response delete() throws Exception {
        EntityManager em = PersistenceUtils.getInstance().getEntityManager();
        try {
            em.getTransaction().begin();
            group = em.merge(group);
            em.remove(group);
            em.getTransaction().commit();
            return Response.status(Response.Status.NO_CONTENT).build();
        }
        finally {
            PersistenceUtils.getInstance().closeEntityManager(em);
        }
    }

    public Response deleteUserGroup() throws Exception {
        EntityManager em = PersistenceUtils.getInstance().getEntityManager();
        try {
            em.getTransaction().begin();
            this.user.getUGroupList().remove(this.group);
            this.group.getUserList().remove(this.user);
            this.user = em.merge(this.user);
            this.group = em.merge(this.group);
            em.getTransaction().commit();
            return Response.status(Response.Status.NO_CONTENT).build();
        }
        finally {
            PersistenceUtils.getInstance().closeEntityManager(em);
        }
    }

    /* (non-Javadoc)
     * @see org.ow2.contrail.federation.federationapi.interfaces.BaseSingle#put(java.lang.String)
     */
    @Override
    public Response put(String content) throws Exception {
        logger.debug("Entering put");
        JSONObject gData = null;
        try {
            gData = new JSONObject(content);
        }
        catch (Exception err) {
            logger.error(err.getMessage());
            logger.error(FederationDBCommon.getStackTrace(err));
            return Response.status(Response.Status.NOT_ACCEPTABLE).build();
        }

        if (gData.has("name"))
            group.setName(gData.getString("name"));
        if (gData.has("description"))
            group.setDescription(gData.getString("description"));

        EntityManager em = PersistenceUtils.getInstance().getEntityManager();
        try {
            em.getTransaction().begin();
            group = em.merge(group);
            em.getTransaction().commit();
            return Response.status(Response.Status.NO_CONTENT).build();
        }
        finally {
            PersistenceUtils.getInstance().closeEntityManager(em);
            logger.debug("Exiting put");
        }
    }

}
