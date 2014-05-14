/**
 *
 */
package org.ow2.contrail.federation.federationapi.resources.impl;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.ow2.contrail.federation.federationapi.resources.IOvfResource;
import org.ow2.contrail.federation.federationapi.resources.IOvfsResource;
import org.ow2.contrail.federation.federationapi.utils.FederationDBCommon;
import org.ow2.contrail.federation.federationdb.jpa.dao.OvfDAO;
import org.ow2.contrail.federation.federationdb.jpa.dao.ProviderDAO;
import org.ow2.contrail.federation.federationdb.jpa.entities.Ovf;
import org.ow2.contrail.federation.federationdb.jpa.entities.Provider;
import org.ow2.contrail.federation.federationdb.utils.PersistenceUtils;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.List;

/**
 * @author ales
 */
@Path("/ovfs")
public class OvfsResource implements IOvfsResource {

    protected static Logger logger =
            Logger.getLogger(OvfsResource.class);

    /**
     * Returns the JSON representation of ovfs .
     *
     * @return
     */
    @GET
    @Produces("application/json")
    public Response get() throws Exception {
        logger.debug("Entering get");
        EntityManager em = PersistenceUtils.getInstance().getEntityManager();
        try {
            Query query = em.createNamedQuery("Ovf.findAll");
            List<Ovf> ovfs = query.getResultList();
            JSONArray UriList = new JSONArray();
            for (Ovf ovf : ovfs) {
                String uri = String.format("/ovfs/%d", ovf.getOvfId());
                JSONObject o = new JSONObject();
                o.put("name", ovf.getName());
                o.put("uri", uri);
                UriList.put(o);
            }
            return Response.ok(UriList.toString()).build();
        }
        finally {
            PersistenceUtils.getInstance().closeEntityManager(em);
            logger.debug("Exiting get");
        }
    }

    @POST
    @Consumes("application/json")
    @Produces("application/json")
    public Response post(String content) throws Exception {
        logger.debug("Entering post");
        JSONObject ovfData = null;
        try {
            ovfData = new JSONObject(content);
        }
        catch (Exception err) {
            logger.error(err.getMessage());
            logger.error(FederationDBCommon.getStackTrace(err));
            return Response.status(Response.Status.NOT_ACCEPTABLE).build();
        }
        String name = (String) ovfData.get("name");
        EntityManager em = PersistenceUtils.getInstance().getEntityManager();
        try {
            Query query = em.createQuery("SELECT COUNT(u) FROM Ovf u WHERE u.name=:name");
            query.setParameter("name", name);
            if ((Long) query.getSingleResult() > 0) {
                // resource is already registered
                return Response.status(Response.Status.CONFLICT).build();
            }

            Ovf ovf = new Ovf();
            if (!ovfData.has("providerId")) {
                logger.error("provider ID has to be provided.");
                return Response.status(Response.Status.NOT_ACCEPTABLE).build();
            }
            String provString = ovfData.getString("providerId");
            // Get Idp
            Provider idp = ProviderDAO.findById(
                    FederationDBCommon.getIdFromString(provString)
            );
            if (idp == null) {
                logger.error("Identity Provider with id " + ovfData.getString("identityProvierId") + " not found.");
                throw new WebApplicationException(Response.Status.NOT_FOUND);
            }
            if (ovfData.has("name"))
                ovf.setName(name);
            else {
                logger.error("Ovf does not have a name attribute.");
                return Response.status(Response.Status.NOT_ACCEPTABLE).build();
            }
            if (ovfData.has("attributes"))
                ovf.setAttributes((String) ovfData.get("attributes"));

            em.getTransaction().begin();
            ovf.setProviderId(idp);
            idp.getOvfList().add(ovf);
            em.persist(ovf);
            idp = em.merge(idp);
            em.getTransaction().commit();

            URI resourceUri = new URI(String.format("/%d", ovf.getOvfId()));
            return Response.created(resourceUri).build();
        }
        finally {
            PersistenceUtils.getInstance().closeEntityManager(em);
            logger.debug("Exiting post");
        }
    }

    /**
     * Sub-resource locator method. Returns the sub-resource object that can handle the remainder
     * of the request.
     *
     * @param ovfId
     * @return
     */
    @Path("/{ovfId}")
    public IOvfResource subResource(@PathParam("ovfId") int ovfId) throws Exception {
        Ovf ovf = OvfDAO.findById(ovfId);
        if (ovf == null) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
        else {
            return new OvfResource(ovf);
        }
    }

}
