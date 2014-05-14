/**
 *
 */
package org.ow2.contrail.federation.federationapi.resources.impl;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.ow2.contrail.federation.federationapi.resources.IUserOvfResource;
import org.ow2.contrail.federation.federationapi.utils.JSONObject;
import org.ow2.contrail.federation.federationdb.jpa.entities.Application;
import org.ow2.contrail.federation.federationdb.jpa.entities.User;
import org.ow2.contrail.federation.federationdb.jpa.entities.UserOvf;
import org.ow2.contrail.federation.federationdb.utils.PersistenceUtils;

import javax.persistence.EntityManager;
import javax.ws.rs.core.Response;
import java.util.ArrayList;

/**
 * @author ales
 */
public class UserOvfResource implements IUserOvfResource {

    protected static Logger logger =
            Logger.getLogger(UserOvfResource.class);

    /**
     * Can be null!
     */
    private User user;
    private Application app;
    private UserOvf ovf;

    /**
     * @param user can be null!
     * @param app
     * @param ovf
     */
    public UserOvfResource(User user, Application app, UserOvf ovf) {
        this.user = user;
        this.app = app;
        this.ovf = ovf;
    }

    public UserOvfResource(User user, UserOvf ovf) {
        this.user = user;
        this.app = null;
        this.ovf = ovf;
    }

    @Override
    public ArrayList<String> getSubresources() throws Exception {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * Get users application's ovf.
     */
    @Override
    public Response get() throws Exception {
        logger.debug("entering get");
        JSONObject ovf = new JSONObject();
        ovf.put("ovfId", this.ovf.getOvfId());
        ovf.put("name", this.ovf.getName());
        ovf.put("attributes", this.ovf.getAttributes());
        ovf.put("content", this.ovf.getContent());
        ovf.put("providerOvfId", String.format("/providers/%s/ovfs/%d", this.ovf.getProviderOvfId().getProviderId().getProviderId(), this.ovf.getProviderOvfId().getOvfId()));
        JSONArray arr = new JSONArray();
        logger.debug("User is not null, creating a list of applications for the user");
        if (user != null) {
            ovf.put("userId", String.format("users/%d", this.user.getUserId()));
            ovf.put("applications", String.format("users/%d/ovfs/%d/applications", this.user.getUserId(), this.ovf.getOvfId()));
        }
        logger.debug("Exiting get");
        return Response.ok(ovf.toString()).build();
    }

    /**
     * Get applications of the ovf.
     */
    public Response getApplications() throws Exception {
        logger.debug("entering get");
        JSONArray json = new JSONArray();
        for (Application app : this.ovf.getApplicationList()) {
            String uri = String.format("/users/%d/applications/%d", user.getUserId(), app.getApplicationId());
            JSONObject o = new JSONObject();
            o.put("name", app.getName());
            o.put("uri", uri);
            json.put(o);
        }
        logger.debug("Exiting get");
        return Response.ok(json.toString()).build();
    }

    /**
     * Delete users application's ovf.
     */
    @Override
    public Response delete() throws Exception {
        logger.debug("Entering delete user ovf resource");
        EntityManager em = PersistenceUtils.getInstance().getEntityManager();
        try {
            em.getTransaction().begin();
            ovf = em.merge(ovf);
            em.remove(ovf);
            if (app != null) {
                logger.debug("Removing ovf from application's list.");
                app.getUserOvfList().remove(ovf);
                app = em.merge(app);
            }
            if (user != null) {
                logger.debug("Removing ovf from user's list.");
                user.getUserOvfList().remove(ovf);
                user = em.merge(user);
            }
            em.getTransaction().commit();
            return Response.status(Response.Status.NO_CONTENT).build();
        }
        finally {
            logger.debug("Exiting delete user ovf resource");
            PersistenceUtils.getInstance().closeEntityManager(em);
        }
    }

    /**
     * Modify users application's ovf.
     */
    @Override
    public Response put(String content) throws Exception {
        logger.debug("Entering put user ovf resource");
        JSONObject json = new JSONObject(content);
        String name = (String) json.get("name");
        if (json.has("name"))
            ovf.setName(name);
        if (json.has("attributes"))
            ovf.setAttributes(json.getString("attributes"));
        if (json.has("content"))
            ovf.setContent(json.getString("content"));
        EntityManager em = PersistenceUtils.getInstance().getEntityManager();
        try {
            em.getTransaction().begin();
            ovf = em.merge(ovf);
            em.getTransaction().commit();
            return Response.status(Response.Status.NO_CONTENT).build();
        }
        finally {
            logger.debug("Exiting put user ovf resource");
            PersistenceUtils.getInstance().closeEntityManager(em);
        }
    }

}
