/**
 *
 */
package org.ow2.contrail.federation.federationapi.resources.impl;

import org.apache.log4j.Logger;
import org.ow2.contrail.federation.federationapi.interfaces.BaseSingle;
import org.ow2.contrail.federation.federationapi.utils.FederationDBCommon;
import org.ow2.contrail.federation.federationapi.utils.JSONObject;
import org.ow2.contrail.federation.federationdb.jpa.entities.IdentityProvider;
import org.ow2.contrail.federation.federationdb.jpa.entities.User;
import org.ow2.contrail.federation.federationdb.jpa.entities.UserhasidentityProvider;
import org.ow2.contrail.federation.federationdb.utils.PersistenceUtils;

import javax.persistence.EntityManager;
import javax.ws.rs.core.Response;
import java.util.ArrayList;

/**
 * @author ales
 */
public class UserIdentityProviderResource implements BaseSingle {

    protected static Logger logger =
            Logger.getLogger(UserIdentityProviderResource.class);

    User user = null;
    IdentityProvider idp = null;
    UserhasidentityProvider uidp = null;

    public UserIdentityProviderResource(User user, IdentityProvider idp, UserhasidentityProvider uidp) {
        this.user = user;
        this.idp = idp;
        this.uidp = uidp;
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
        attr.put("userId", String.format("/users/%d", this.user.getUserId()));
        attr.put("identityProviderId", String.format("/idps/%d", this.idp.getIdentityProviderId()));
        attr.put("identity", this.uidp.getIdentity());
        attr.put("attributes", this.uidp.getAttributes());
        return Response.ok(attr.toString()).build();
    }

    @Override
    public Response delete() throws Exception {
        logger.debug("Entering delete user idp.");
        EntityManager em = PersistenceUtils.getInstance().getEntityManager();
        try {
            em.getTransaction().begin();
            this.user.getUserhasidentityProviderList().remove(this.uidp);
            this.idp.getUserhasidentityProviderList().remove(this.uidp);
            this.user = em.merge(this.user);
            this.idp = em.merge(this.idp);
            uidp = em.merge(uidp);
            em.remove(uidp);
            em.getTransaction().commit();
            return Response.status(Response.Status.NO_CONTENT).build();
        }
        finally {
            logger.debug("Exiting delete user idp.");
            PersistenceUtils.getInstance().closeEntityManager(em);
        }
    }

    @Override
    public Response put(String content) throws Exception {
        logger.debug("Entering user put");
        JSONObject idData = null;
        try {
            idData = new JSONObject(content);
        }
        catch (Exception err) {
            logger.error(err.getMessage());
            logger.error(FederationDBCommon.getStackTrace(err));
            return Response.status(Response.Status.NOT_ACCEPTABLE).build();
        }
        EntityManager em = PersistenceUtils.getInstance().getEntityManager();
        try {
            if (idData.has("identity"))
                uidp.setIdentity(idData.getString("identity"));
            if (idData.has("attributes"))
                uidp.setAttributes(idData.getString("attributes"));
            em.getTransaction().begin();
            uidp = em.merge(uidp);
            em.getTransaction().commit();
            return Response.status(Response.Status.NO_CONTENT).build();
        }
        catch (Exception err) {
            logger.error(err.getMessage());
            return Response.serverError().build();
        }
        finally {
            logger.debug("Exiting user put");
            PersistenceUtils.getInstance().closeEntityManager(em);
        }
    }

}
