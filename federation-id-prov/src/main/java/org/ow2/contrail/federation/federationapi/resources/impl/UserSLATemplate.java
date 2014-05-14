/**
 *
 */
package org.ow2.contrail.federation.federationapi.resources.impl;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.ow2.contrail.federation.federationapi.resources.IUserSLATemplate;
import org.ow2.contrail.federation.federationapi.utils.FederationDBCommon;
import org.ow2.contrail.federation.federationapi.utils.JSONObject;
import org.ow2.contrail.federation.federationdb.jpa.dao.SLATemplateDAO;
import org.ow2.contrail.federation.federationdb.jpa.entities.SLATemplate;
import org.ow2.contrail.federation.federationdb.jpa.entities.User;
import org.ow2.contrail.federation.federationdb.jpa.entities.UserSLA;
import org.ow2.contrail.federation.federationdb.utils.PersistenceUtils;

import javax.persistence.EntityManager;
import javax.ws.rs.core.Response;
import java.util.ArrayList;

/**
 * @author ales.cernivec@xlab.si
 */
public class UserSLATemplate implements IUserSLATemplate {

    protected static Logger logger =
            Logger.getLogger(UserSLATemplate.class);

    private org.ow2.contrail.federation.federationdb.jpa.entities.UserSLATemplate slat = null;
    private User user = null;

    public UserSLATemplate(User user, org.ow2.contrail.federation.federationdb.jpa.entities.UserSLATemplate slat) {
        this.slat = slat;
        this.user = user;
    }

    @Override
    public ArrayList<String> getSubresources() throws Exception {
        return null;
    }

    @Override
    public Response get() throws Exception {
        logger.debug("Entering get");
        JSONObject slat = null;
        slat = new JSONObject();
        slat.put("SLATemplateId", String.format("/users/%d/slats/%d", this.user.getUserId(), this.slat.getSLATemplateId()));
        slat.put("url", this.slat.getUrl());
        slat.put("userid", String.format("/users/%d", this.slat.getUserId().getUserId()));
        slat.put("content", this.slat.getContent());
        if (this.slat.getSlatId() != null)
            slat.put("slatId", String.format("/slats/%d", this.slat.getSlatId().getSlatId()));
        else
            slat.put("slatId", JSONObject.NULL);
        slat.put("userSLAs", String.format("/users/%d/slats/%d/slas", this.user.getUserId(), this.slat.getSLATemplateId()));
        logger.debug("Exiting get");
        return Response.ok(slat.toString()).build();
    }

    /**
     * Return user slas corresponding to these SLATs.
     */
    public Response getSLATsUserSLA() throws Exception {
        logger.debug("Entering get");
        JSONArray json = new JSONArray();
        for (UserSLA userSla : this.slat.getUserSLAList()) {
            String uri = String.format("/users/%d/slas/%d", user.getUserId(), userSla.getSLAId());
            JSONObject o = new JSONObject();
            o.put("name", userSla.getSla());
            o.put("uri", uri);
            json.put(o);
        }
        return Response.ok(json.toString()).build();
    }


    @Override
    public Response delete() throws Exception {
        logger.debug("Entering delete");
        EntityManager em = PersistenceUtils.getInstance().getEntityManager();
        try {
            logger.debug("deleting userSLA");
            em.getTransaction().begin();
            SLATemplate slatBefore = this.slat.getSlatId();
            if (slatBefore != null) {
                logger.debug("Removing UserSLATemplate from SLATemplate" + slatBefore.getSlatId());
                slatBefore.getUserSLATemplateList().remove(this.slat);
                slatBefore = em.merge(slatBefore);
            }
            slat = em.merge(slat);
            em.remove(slat);
            user.getUserSLATemplateList().remove(slat);
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
        JSONObject slatdata = null;
        try {
            slatdata = new JSONObject(content);
        }
        catch (Exception err) {
            logger.error(err.getMessage());
            logger.error(FederationDBCommon.getStackTrace(err));
            return Response.status(Response.Status.NOT_ACCEPTABLE).build();
        }
        EntityManager em = PersistenceUtils.getInstance().getEntityManager();
        try {
            em.getTransaction().begin();

            if (slatdata.has("url")) {
                this.slat.setUrl(slatdata.getString("url"));
            }
            if (slatdata.has("slatId")) {
                SLATemplate slat = SLATemplateDAO.findById(FederationDBCommon.getIdFromString(slatdata.getString("slatId")));
                if (slat != null) {
                    logger.debug("Found SLATemplate with id " + slat.getSlatId());
                    SLATemplate slatBefore = this.slat.getSlatId();
                    if (slatBefore != null) {
                        logger.debug("Removing UserSLATemplate from SLATemplate" + slatBefore.getSlatId());
                        slatBefore.getUserSLATemplateList().remove(this.slat);
                        slatBefore = em.merge(slatBefore);
                    }
                    this.slat.setSlatId(slat);
                    slat.getUserSLATemplateList().add(this.slat);
                    slat = em.merge(slat);
                }
                else {
                    logger.error("Could not found SLATemplate with id: " + slatdata.getString("slatId"));
                }
            }

            if (slatdata.has("content")) {
                this.slat.setContent(slatdata.getString("content"));
            }
            logger.debug("merging UserSLATemplate");
            slat = em.merge(slat);
            em.getTransaction().commit();
            logger.debug("Exiting put");
            return Response.status(Response.Status.NO_CONTENT).build();
        }
        finally {
            PersistenceUtils.getInstance().closeEntityManager(em);
        }
    }

}
