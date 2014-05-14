/**
 *
 */
package org.ow2.contrail.federation.federationapi.resources.impl;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.ow2.contrail.federation.federationapi.resources.IIdentityProviderResource;
import org.ow2.contrail.federation.federationapi.utils.FederationDBCommon;
import org.ow2.contrail.federation.federationapi.utils.JSONObject;
import org.ow2.contrail.federation.federationdb.jpa.entities.IdentityProvider;
import org.ow2.contrail.federation.federationdb.jpa.entities.UserhasidentityProvider;
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
public class IdentityProviderResource implements IIdentityProviderResource {

    protected static Logger logger =
            Logger.getLogger(IdentityProviderResource.class);

    protected IdentityProvider idp = null;

    public IdentityProviderResource(IdentityProvider idp) {
        this.idp = idp;
    }

    /* (non-Javadoc)
     * @see org.ow2.contrail.federation.federationapi.interfaces.BaseSingle#getSubresources()
     */
    @Override
    public ArrayList<String> getSubresources() throws Exception {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see org.ow2.contrail.federation.federationapi.interfaces.BaseSingle#get()
     */
    @Override
    public Response get() throws Exception {
        logger.debug("Entering get");
        JSONObject idp = null;
        idp = new JSONObject();
        idp.put("identityProviderId", this.idp.getIdentityProviderId());
        idp.put("providerURI", this.idp.getProviderURI());
        idp.put("description", this.idp.getDescription());
        idp.put("providerName", this.idp.getProviderName());
        idp.put("users", String.format("/idps/%d/users", this.idp.getIdentityProviderId()));
        logger.debug("Exiting get");
        return Response.ok(idp.toString()).build();
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
        for (UserhasidentityProvider user : this.idp.getUserhasidentityProviderList()) {
            String uri = String.format("/users/%d", user.getUser().getUserId());
            JSONObject o = new JSONObject();
            o.put("name", user.getUser().getUsername());
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
            idp = em.merge(idp);
            em.remove(idp);
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
        JSONObject idpData = null;
        try {
            idpData = new JSONObject(content);
        }
        catch (Exception err) {
            logger.error(err.getMessage());
            logger.error(FederationDBCommon.getStackTrace(err));
            return Response.status(Response.Status.NOT_ACCEPTABLE).build();
        }

        if (idpData.has("description"))
            this.idp.setDescription(idpData.getString("description"));
        if (idpData.has("providerURI"))
            this.idp.setProviderURI(idpData.getString("providerURI"));
        if (idpData.has("providerName"))
            this.idp.setProviderName(idpData.getString("providerName"));

        EntityManager em = PersistenceUtils.getInstance().getEntityManager();
        try {
            logger.debug("Commiting new Idp");
            em.getTransaction().begin();
            idp = em.merge(this.idp);
            em.getTransaction().commit();
            return Response.status(Response.Status.NO_CONTENT).build();
        }
        finally {
            PersistenceUtils.getInstance().closeEntityManager(em);
            logger.debug("Exiting put");
        }
    }

}
