/**
 *
 */
package org.ow2.contrail.federation.federationapi.resources.impl;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.ow2.contrail.federation.federationapi.resources.IApplicationResource;
import org.ow2.contrail.federation.federationapi.utils.FederationDBCommon;
import org.ow2.contrail.federation.federationapi.utils.JSONObject;
import org.ow2.contrail.federation.federationdb.jpa.dao.UserOvfDAO;
import org.ow2.contrail.federation.federationdb.jpa.entities.Application;
import org.ow2.contrail.federation.federationdb.jpa.entities.User;
import org.ow2.contrail.federation.federationdb.jpa.entities.UserOvf;
import org.ow2.contrail.federation.federationdb.utils.PersistenceUtils;

import javax.persistence.EntityManager;
import javax.ws.rs.Consumes;
import javax.ws.rs.PUT;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.util.ArrayList;

/**
 * @author ales
 */
public class ApplicationResource implements IApplicationResource {

    protected static Logger logger =
            Logger.getLogger(ApplicationResource.class);

    protected Application app = null;
    protected User user = null;

    public ApplicationResource(Application app, User user) {
        this.app = app;
        this.user = user;
    }

    public ApplicationResource(Application app) {
        this.app = app;
        this.user = null;
    }

    @Override
    public ArrayList<String> getSubresources() throws Exception {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * Gets the info on application. If user is not given, list users who
     * use this application.
     */
    @Override
    public Response get() throws Exception {
        JSONObject app = null;
        app = new JSONObject();
        app.put("applicationId", this.app.getApplicationId());
        app.put("deploymentDesc", this.app.getDeploymentDesc());
        app.put("name", this.app.getName());
        app.put("applicationOvf", this.app.getApplicationOvf());
        app.put("attributes", this.app.getAttributes());
        app.put("state", this.app.getState());
        JSONArray arr = new JSONArray();
        for (UserOvf ovf : this.app.getUserOvfList()) {
            arr.put(String.format("users/%d/ovfs/%d", ovf.getUserId().getUserId(), ovf.getOvfId()));
        }
        app.put("ovfs", arr.toString());
        arr = new JSONArray();
        if (user == null) {
            logger.debug("Listing all users who use this application.");
            // No user is given, list users of this application
            for (User appuser : this.app.getUserList()) {
                arr.put(String.format("users/%d", appuser.getUserId()));
            }
            app.put("users", arr.toString());
        }
        return Response.ok(app.toString()).build();
    }

    @Override
    public Response delete() throws Exception {
        EntityManager em = PersistenceUtils.getInstance().getEntityManager();
        try {
            em.getTransaction().begin();
            app = em.merge(app);
            em.remove(app);
            if (user == null) {
                // No user is given, list users of this application
                for (User appuser : this.app.getUserList()) {
                    appuser.getApplicationList().remove(app);
                    appuser = em.merge(appuser);
                    logger.debug("No user given, deleting for user " + appuser.getUserId());
                }
            }
            else {
                logger.debug("User given, deleting only for user " + user.getUserId());
                user.getApplicationList().remove(app);
                user = em.merge(user);
            }
            em.getTransaction().commit();
            return Response.status(Response.Status.NO_CONTENT).build();
        }
        finally {
            PersistenceUtils.getInstance().closeEntityManager(em);
        }
    }

    @PUT
    @Consumes("application/json")
    @Produces("application/json")
    //    @Path("{appId}")
    public Response put(/*@PathParam("appId") int appId,*/ String content) throws Exception {
        JSONObject appData = null;
        try {
            appData = new JSONObject(content);
        }
        catch (Exception err) {
            logger.error(err.getMessage());
            logger.error(FederationDBCommon.getStackTrace(err));
            return Response.status(Response.Status.NOT_ACCEPTABLE).build();
        }

        if (appData.has("name"))
            this.app.setName(appData.getString("name"));
        if (appData.has("deploymentDesc"))
            this.app.setDeploymentDesc(appData.getString("deploymentDesc"));
        if (appData.has("applicationOvf"))
            this.app.setApplicationOvf(appData.getString("applicationOvf"));
        if (appData.has("attributes"))
            this.app.setAttributes(appData.getString("attributes"));
        if (appData.has("state"))
            this.app.setState(appData.getString("state"));
        if (appData.has("ovfs")) {
            logger.debug("got array of OVFs");
            JSONArray array = appData.getJSONArray("ovfs");
            logger.debug("Updating the list of ovfs for user.");
            EntityManager em = PersistenceUtils.getInstance().getEntityManager();
            em.getTransaction().begin();
            for (UserOvf userovf : this.app.getUserOvfList()) {
                userovf.getApplicationList().remove(this.app);
                userovf = em.merge(userovf);
            }

            this.app.getUserOvfList().clear();
            for (int i = 0; i < array.length(); ++i) {
                String str = array.getString(i);
                int ovfid = Integer.parseInt(str.substring(str.lastIndexOf("/") + 1));
                logger.debug("Got UserOvf id " + ovfid);
                UserOvf userovf = UserOvfDAO.findById(ovfid);
                if (userovf == null) {
                    logger.error("Could not find userOvf with id " + ovfid);
                    throw new WebApplicationException(Response.Status.NOT_FOUND);
                }
                this.app.getUserOvfList().add(userovf);
                userovf.getApplicationList().add(this.app);
                userovf = em.merge(userovf);
            }
            this.app = em.merge(this.app);
            em.getTransaction().commit();
            PersistenceUtils.getInstance().closeEntityManager(em);
        }

        EntityManager em = PersistenceUtils.getInstance().getEntityManager();
        try {
            em.getTransaction().begin();
            this.app = em.merge(this.app);
            em.getTransaction().commit();
            return Response.status(Response.Status.NO_CONTENT).build();
        }
        finally {
            PersistenceUtils.getInstance().closeEntityManager(em);
        }
    }

    @Override
    public Response getOvfs() throws Exception {
        logger.debug("Entering getOvfs");
        EntityManager em = PersistenceUtils.getInstance().getEntityManager();
        try {
            JSONArray UriList = new JSONArray();
            if (user == null) {
                logger.debug("User is null, listing ovfs per app per user");
                // for each app user
                for (User appuser : app.getUserList()) {
                    // for each app
                    for (UserOvf ovf : app.getUserOvfList()) {
                        String uri = String.format("/users/%d/ovfs/%d",
                                appuser.getUserId(),
                                ovf.getOvfId());
                        JSONObject o = new JSONObject();
                        o.put("name", ovf.getName());
                        o.put("uri", uri);
                        UriList.put(o);
                    }
                }
            }
            else {
                logger.debug("User is not null, listing ovfs per app of the user " + this.user.getUserId());
                // for each app
                for (UserOvf ovf : app.getUserOvfList()) {
                    String uri = String.format("/users/%d/ovfs/%d",
                            user.getUserId(),
                            ovf.getOvfId());
                    JSONObject o = new JSONObject();
                    o.put("name", ovf.getName());
                    o.put("uri", uri);
                    UriList.put(o);
                }
            }
            return Response.ok(UriList.toString()).build();
        }
        finally {
            PersistenceUtils.getInstance().closeEntityManager(em);
            logger.debug("Exiting getOvfs");
        }
    }

    //	/**
    //	 * Create new User OVF.
    //	 */
    //	@Override
    //	public Response postOvfs(String content) throws Exception {
    //		logger.debug("Entering post of users application's OVF");
    //		JSONObject ovfjson = null;
    //		try{
    //			ovfjson = new JSONObject(content);
    //		}
    //		catch (JSONException err){
    //			logger.error(err.getMessage());
    //			logger.error(FederationDBCommon.getStackTrace(err));
    //			return Response.status(Response.Status.NOT_ACCEPTABLE).build();
    //		}
    //		EntityManager em = PersistenceUtils.getInstance().getEntityManager();
    //		try {
    //			UserOvf userovf = new UserOvf();
    //			if(ovfjson.has("name"))
    //				userovf.setName(ovfjson.getString("name"));
    //			if(ovfjson.has("attributes"))
    //				userovf.setAttributes(ovfjson.getString("attributes"));
    //			em.getTransaction().begin();
    //			em.persist(userovf);
    //			app.getUserOvfList().add(userovf);
    //			app = em.merge(app);
    //			em.getTransaction().commit();
    //			URI resourceUri = new URI(String.format("/users/%d/applications/%d/ovfs/%d", user.getUserId(), app.getApplicationId(), userovf.getOvfId()));
    //			logger.debug("Exiting post of users application's OVF");
    //			return Response.created(resourceUri).build();
    //		}
    //		catch(Exception err){
    //			logger.error(err.getMessage());
    //			return Response.serverError().build();
    //		}
    //		finally {
    //			PersistenceUtils.getInstance().closeEntityManager(em);
    //		}
    //	}

    @Override
    public Response getOvf(int ovfId) throws Exception {
        UserOvf ovf = UserOvfDAO.findById(ovfId);
        if (ovf == null) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
        else {
            return new UserOvfResource(this.user, this.app, ovf).get();
        }
    }

    public Response deleteOvf(int ovfId) throws Exception {
        UserOvf ovf = UserOvfDAO.findById(ovfId);
        if (ovf == null) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
        else {
            return new UserOvfResource(this.user, this.app, ovf).delete();
        }
    }


}
