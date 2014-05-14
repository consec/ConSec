/**
 *
 */
package org.ow2.contrail.federation.federationapi.resources.impl;

import org.apache.log4j.Logger;
import org.ow2.contrail.federation.federationapi.resources.IUserSLA;
import org.ow2.contrail.federation.federationapi.utils.FederationDBCommon;
import org.ow2.contrail.federation.federationapi.utils.JSONObject;
import org.ow2.contrail.federation.federationdb.jpa.dao.UserSLATemplateDAO;
import org.ow2.contrail.federation.federationdb.jpa.entities.User;
import org.ow2.contrail.federation.federationdb.utils.PersistenceUtils;

import javax.persistence.EntityManager;
import javax.ws.rs.core.Response;
import java.util.ArrayList;

/**
 * @author ales.cernivec@xlab.si
 */
public class UserSLA implements IUserSLA {

    protected static Logger logger =
            Logger.getLogger(UserSLA.class);

    private org.ow2.contrail.federation.federationdb.jpa.entities.UserSLA userSLA = null;
    private User user = null;

    public UserSLA(User user, org.ow2.contrail.federation.federationdb.jpa.entities.UserSLA userSLA) {
        this.userSLA = userSLA;
        this.user = user;
    }

    @Override
    public ArrayList<String> getSubresources() throws Exception {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public Response get() throws Exception {
        JSONObject sla = null;
        sla = new JSONObject();
        sla.put("id", String.format("/users/%d/slas/%d", this.user.getUserId(), this.userSLA.getSLAId()));
        sla.put("sla", this.userSLA.getSla());
        sla.put("templateurl", this.userSLA.getTemplateURL());
        sla.put("userId", String.format("/users/%d", this.userSLA.getUserId().getUserId()));
        sla.put("content", this.userSLA.getContent());
        if (this.userSLA.getSLATemplateId() != null)
            sla.put("slatId", String.format("/users/%d/slats/%d", this.user.getUserId(), this.userSLA.getSLATemplateId().getSLATemplateId()));
        else
            sla.put("slatId", JSONObject.NULL);
        return Response.ok(sla.toString()).build();
    }

    /* (non-Javadoc)
     * @see org.ow2.contrail.federation.federationapi.interfaces.BaseSingle#delete()
     */
    @Override
    public Response delete() throws Exception {
        logger.debug("Entering delete");
        EntityManager em = PersistenceUtils.getInstance().getEntityManager();
        try {
            //user.getUserSLAList().remove(userSLA);
            logger.debug("deleting userSLA");
            em.getTransaction().begin();
            org.ow2.contrail.federation.federationdb.jpa.entities.UserSLATemplate userSlat = this.userSLA.getSLATemplateId();
            if (userSlat != null) {
                logger.debug("Removing UserSLA from UserSLATemplate" + userSlat.getSLATemplateId());
                userSlat.getUserSLAList().remove(this.userSLA);
                userSlat = em.merge(userSlat);
            }
            userSLA = em.merge(userSLA);
            em.remove(userSLA);
            user.getUserSLAList().remove(userSLA);
            user = em.merge(user);
            em.getTransaction().commit();
            logger.debug("Exiting delete");
            return Response.status(Response.Status.NO_CONTENT).build();
        }
        finally {
            PersistenceUtils.getInstance().closeEntityManager(em);
        }
    }

    @Override
    public Response put(String content) throws Exception {
        logger.debug("Entering put");
        JSONObject slaData = null;
        try {
            slaData = new JSONObject(content);
        }
        catch (Exception err) {
            logger.error(err.getMessage());
            logger.error(FederationDBCommon.getStackTrace(err));
            return Response.status(Response.Status.NOT_ACCEPTABLE).build();
        }
        EntityManager em = PersistenceUtils.getInstance().getEntityManager();
        try {
            if (slaData.has("sla")) {
                this.userSLA.setSla(slaData.getString("sla"));
            }
            if (slaData.has("content")) {
                this.userSLA.setContent(slaData.getString("content"));
            }
            if (slaData.has("templateurl")) {
                this.userSLA.setTemplateURL(slaData.getString("templateurl"));
            }
            if (slaData.has("slatId")) {
                org.ow2.contrail.federation.federationdb.jpa.entities.UserSLATemplate slat =
                        UserSLATemplateDAO.findById(FederationDBCommon.getIdFromString(slaData.getString("slatId")));
                if (slat != null) {
                    logger.debug("Found UserSLATemplate with id " + slat.getSLATemplateId());
                    org.ow2.contrail.federation.federationdb.jpa.entities.UserSLATemplate slatBefore =
                            this.userSLA.getSLATemplateId();
                    if (slatBefore != null) {
                        logger.debug("Removing UserSLA from UserSLATemplate" + slatBefore.getSLATemplateId());
                        slatBefore.getUserSLAList().remove(this.userSLA);
                        slatBefore = em.merge(slatBefore);
                    }
                    this.userSLA.setSLATemplateId(slat);
                    slat.getUserSLAList().add(this.userSLA);
                    slat = em.merge(slat);
                }
                else {
                    logger.error("Could not found SLATemplate with id: " + slaData.getString("slatId"));
                }
            }
            logger.debug("merging");
            em.getTransaction().begin();
            userSLA = em.merge(userSLA);
            em.getTransaction().commit();
            logger.debug("Exiting put");
            return Response.status(Response.Status.NO_CONTENT).build();
        }
        finally {
            PersistenceUtils.getInstance().closeEntityManager(em);
        }
    }

}
