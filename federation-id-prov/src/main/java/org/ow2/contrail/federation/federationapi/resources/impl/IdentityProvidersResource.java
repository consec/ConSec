/**
 *
 */
package org.ow2.contrail.federation.federationapi.resources.impl;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.ow2.contrail.federation.federationapi.resources.IIdentityProviderResource;
import org.ow2.contrail.federation.federationapi.resources.IIdentityProvidersResource;
import org.ow2.contrail.federation.federationapi.utils.FederationDBCommon;
import org.ow2.contrail.federation.federationdb.jpa.dao.IdentityProviderDAO;
import org.ow2.contrail.federation.federationdb.jpa.entities.IdentityProvider;
import org.ow2.contrail.federation.federationdb.utils.PersistenceUtils;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.ws.rs.Path;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.List;

/**
 * @author ales
 */

@Path("/idps")
public class IdentityProvidersResource implements IIdentityProvidersResource {

    protected static Logger logger =
            Logger.getLogger(IdentityProvidersResource.class);

    /* (non-Javadoc)
     * @see org.ow2.contrail.federation.federationapi.interfaces.BaseCollection#get()
     */
    @Override
    public Response get() throws Exception {
        EntityManager em = PersistenceUtils.getInstance().getEntityManager();
        try {
            Query query = em.createNamedQuery("IdentityProvider.findAll");
            List<IdentityProvider> idpList = query.getResultList();
            JSONArray UriList = new JSONArray();
            for (IdentityProvider idp : idpList) {
                String uri = String.format("/idps/%d", idp.getIdentityProviderId());
                JSONObject o = new JSONObject();
                o.put("name", idp.getProviderName());
                o.put("uri", uri);
                UriList.put(o);
            }
            return Response.ok(UriList.toString()).build();
        }
        finally {
            PersistenceUtils.getInstance().closeEntityManager(em);
        }
    }

    /* (non-Javadoc)
     * @see org.ow2.contrail.federation.federationapi.interfaces.BaseCollection#post(java.lang.String)
     */
    @Override
    public Response post(String content) throws Exception {
        logger.debug("Entering post");
        JSONObject idpData = null;
        try {
            idpData = new JSONObject(content);
        }
        catch (Exception err) {
            logger.error(err.getMessage());
            logger.error(FederationDBCommon.getStackTrace(err));
            return Response.status(Response.Status.NOT_ACCEPTABLE).build();
        }
        String name = (String) idpData.get("providerName");
        EntityManager em = PersistenceUtils.getInstance().getEntityManager();
        try {
            Query query = em.createQuery("SELECT COUNT(i.providerName) FROM IdentityProvider i WHERE i.providerName = :name");
            query.setParameter("name", name);
            if ((Long) query.getSingleResult() > 0) {
                // resource is already registered
                logger.error("Resource is already registered. Can not register the same resource.");
                return Response.status(Response.Status.CONFLICT).build();
            }

            IdentityProvider idp = new IdentityProvider();
            if (idpData.has("providerURI"))
                idp.setProviderURI(idpData.getString("providerURI"));
            if (idpData.has("description"))
                idp.setDescription(idpData.getString("description"));
            if (idpData.has("providerName"))
                idp.setProviderName(idpData.getString("providerName"));
            em.getTransaction().begin();
            em.persist(idp);
            em.getTransaction().commit();

            URI resourceUri = new URI(String.format("/%d", idp.getIdentityProviderId()));
            return Response.created(resourceUri).build();
        }
        catch (Exception err) {
            logger.error(err.getMessage());
            return Response.serverError().build();
        }
        finally {
            PersistenceUtils.getInstance().closeEntityManager(em);
            logger.debug("Exiting post");
        }
    }

    /* (non-Javadoc)
     * @see org.ow2.contrail.federation.federationapi.interfaces.BaseCollection#subResource(java.lang.String)
     */
    @Override
    public IIdentityProviderResource subResource(int id) throws Exception {
        IdentityProvider idp = IdentityProviderDAO.findById(id);
        if (idp == null) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
        else {
            return new IdentityProviderResource(idp);
        }
    }

}
