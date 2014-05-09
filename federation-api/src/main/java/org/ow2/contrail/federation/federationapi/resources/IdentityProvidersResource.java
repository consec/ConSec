/**
 *
 */
package org.ow2.contrail.federation.federationapi.resources;

import org.apache.log4j.Logger;
import org.consec.federationdb.model.IdentityProvider;
import org.consec.federationdb.utils.EMF;
import org.json.JSONArray;
import org.ow2.contrail.federation.federationapi.utils.JSONObject;
import org.ow2.contrail.federation.federationapi.utils.RestUriBuilder;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.List;
import java.util.UUID;

/**
 * @author ales
 */

@Path("/idps")
public class IdentityProvidersResource {

    protected static Logger logger =
            Logger.getLogger(IdentityProvidersResource.class);

    /* (non-Javadoc)
      * @see org.ow2.contrail.federation.federationapi.interfaces.BaseCollection#get()
      */
    @GET
    @Produces("application/json")
    public Response get() throws Exception {
        EntityManager em = EMF.createEntityManager();
        try {
            Query query = em.createNamedQuery("IdentityProvider.findAll");
            List<IdentityProvider> idpList = query.getResultList();
            JSONArray UriList = new JSONArray();
            for (IdentityProvider idp : idpList) {
                JSONObject o = new JSONObject();
                o.put("name", idp.getName());
                o.put("uri", RestUriBuilder.getIdpUri(idp));
                UriList.put(o);
            }
            return Response.ok(UriList.toString()).build();
        }
        finally {
            EMF.closeEntityManager(em);
        }
    }

    /* (non-Javadoc)
      * @see org.ow2.contrail.federation.federationapi.interfaces.BaseCollection#post(java.lang.String)
      */
    @POST
    @Consumes("application/json")
    @Produces("application/json")
    public Response post(JSONObject idpData) throws Exception {
        logger.debug("Entering post");
        String name = (String) idpData.get("providerName");
        EntityManager em = EMF.createEntityManager();
        try {
            Query query = em.createQuery("SELECT COUNT(i.providerName) FROM IdentityProvider i WHERE i.providerName = :name");
            query.setParameter("name", name);
            if ((Long) query.getSingleResult() > 0) {
                // resource is already registered
                logger.error("Resource is already registered. Can not register the same resource.");
                return Response.status(Response.Status.CONFLICT).build();
            }

            String idpUuid = idpData.has("uuid") ? idpData.getString("uuid") : UUID.randomUUID().toString();

            IdentityProvider idp = new IdentityProvider(idpUuid);
            if (idpData.has("providerURI"))
                idp.setUri(idpData.getString("providerURI"));
            if (idpData.has("description"))
                idp.setDescription(idpData.getString("description"));
            if (idpData.has("providerName"))
                idp.setName(idpData.getString("providerName"));
            idp.setAttributes("{}");
            em.getTransaction().begin();
            em.persist(idp);
            em.getTransaction().commit();

            URI resourceUri = new URI(String.format("/%s", idp.getIdpId()));
            return Response.created(resourceUri).build();
        }
        catch (Exception err) {
            logger.error(err.getMessage());
            return Response.serverError().build();
        }
        finally {
            EMF.closeEntityManager(em);
            logger.debug("Exiting post");
        }
    }
}
