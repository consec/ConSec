package org.ow2.contrail.federation.federationapi.resources.impl;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.ow2.contrail.federation.federationapi.resources.IURoleResource;
import org.ow2.contrail.federation.federationapi.utils.FederationDBCommon;
import org.ow2.contrail.federation.federationapi.utils.JSONObject;
import org.ow2.contrail.federation.federationdb.jpa.entities.URole;
import org.ow2.contrail.federation.federationdb.jpa.entities.User;
import org.ow2.contrail.federation.federationdb.utils.PersistenceUtils;

import javax.persistence.EntityManager;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import java.util.ArrayList;

public class URoleResource implements IURoleResource {

    protected static Logger logger =
            Logger.getLogger(URoleResource.class);

    protected User user = null;
    protected URole role = null;

    public URoleResource(User user, URole role) {
        this.user = user;
        this.role = role;
    }

    public URoleResource(URole role) {
        this.user = null;
        this.role = role;
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
        attr.put("roleId", this.role.getRoleId());
        attr.put("name", this.role.getName());
        attr.put("description", this.role.getDescription());
        attr.put("acl", this.role.getAcl());
        attr.put("users", String.format("/roles/%d/users", this.role.getRoleId()));
        return Response.ok(attr.toString()).build();
    }

    @GET
    @Produces("application/json")
    @Path("/users")
    public Response getUsers() throws Exception {
        JSONArray attr = null;
        attr = new JSONArray();
        for (User user : this.role.getUserList()) {
            String uri = String.format("/users/%d", user.getUserId());
            JSONObject o = new JSONObject();
            o.put("name", role.getName());
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
            role = em.merge(role);
            em.remove(role);
            em.getTransaction().commit();
            return Response.status(Response.Status.NO_CONTENT).build();
        }
        finally {
            PersistenceUtils.getInstance().closeEntityManager(em);
        }
    }

    /**
     * Deletes a user role.
     *
     * @return
     * @throws Exception
     */
    public Response deleteUserRole() throws Exception {
        logger.debug("Entering delete user role");
        EntityManager em = PersistenceUtils.getInstance().getEntityManager();
        try {
            em.getTransaction().begin();
            user.getURoleList().remove(this.role);
            this.role.getUserList().remove(this.user);
            role = em.merge(role);
            user = em.merge(user);
            em.getTransaction().commit();
            return Response.status(Response.Status.NO_CONTENT).build();
        }
        finally {
            logger.debug("Exiting delete user role");
            PersistenceUtils.getInstance().closeEntityManager(em);
        }
    }

    @Override
    public Response put(String content) throws Exception {
        logger.debug("Entering put");
        JSONObject roleData = null;
        try {
            roleData = new JSONObject(content);
        }
        catch (Exception err) {
            logger.error(err.getMessage());
            logger.error(FederationDBCommon.getStackTrace(err));
            return Response.status(Response.Status.NOT_ACCEPTABLE).build();
        }

        if (roleData.has("name"))
            role.setName(roleData.getString("name"));
        if (roleData.has("description"))
            role.setDescription(roleData.getString("description"));
        if (roleData.has("acl"))
            role.setAcl(roleData.getString("acl"));

        EntityManager em = PersistenceUtils.getInstance().getEntityManager();
        try {
            em.getTransaction().begin();
            role = em.merge(role);
            em.getTransaction().commit();
            return Response.status(Response.Status.NO_CONTENT).build();
        }
        finally {
            PersistenceUtils.getInstance().closeEntityManager(em);
            logger.debug("Exiting put");
        }
    }

}
