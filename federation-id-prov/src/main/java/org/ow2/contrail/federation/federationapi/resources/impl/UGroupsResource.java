/**
 *
 */
package org.ow2.contrail.federation.federationapi.resources.impl;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.ow2.contrail.federation.federationapi.resources.IUGroupResource;
import org.ow2.contrail.federation.federationapi.resources.IUGroupsResource;
import org.ow2.contrail.federation.federationapi.utils.FederationDBCommon;
import org.ow2.contrail.federation.federationdb.jpa.dao.UGroupDAO;
import org.ow2.contrail.federation.federationdb.jpa.entities.UGroup;
import org.ow2.contrail.federation.federationdb.utils.PersistenceUtils;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.ws.rs.Path;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import java.net.URI;
import java.util.List;

/**
 * @author ales
 */
@Path("/groups")
public class UGroupsResource implements IUGroupsResource {

    protected static Logger logger =
            Logger.getLogger(UGroupsResource.class);

    /* (non-Javadoc)
     * @see org.ow2.contrail.federation.federationapi.interfaces.BaseCollection#get()
     */
    @Override
    public Response get() throws Exception {
        EntityManager em = PersistenceUtils.getInstance().getEntityManager();
        try {
            Query query = em.createNamedQuery("UGroup.findAll");
            List<UGroup> gList = query.getResultList();
            JSONArray UriList = new JSONArray();
            for (UGroup g : gList) {
                String uri = String.format("/groups/%d", g.getGroupId());
                JSONObject o = new JSONObject();
                o.put("name", g.getName());
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
        JSONObject gData = null;
        try {
            gData = new JSONObject(content);
        }
        catch (Exception err) {
            logger.error(err.getMessage());
            logger.error(FederationDBCommon.getStackTrace(err));
            return Response.status(Response.Status.NOT_ACCEPTABLE).build();
        }
        String name = (String) gData.get("name");
        EntityManager em = PersistenceUtils.getInstance().getEntityManager();
        try {
            Query query = em.createQuery("SELECT COUNT(u) FROM UGroup u WHERE u.name=:name");
            query.setParameter("name", name);
            if ((Long) query.getSingleResult() > 0) {
                // resource is already registered
                return Response.status(Response.Status.CONFLICT).build();
            }

            UGroup group = new UGroup();
            if (gData.has("name"))
                group.setName(name);
            else {
                logger.error("Group does not have a name attribute.");
                return Response.status(Response.Status.NOT_ACCEPTABLE).build();
            }

            if (gData.has("name"))
                group.setName((String) gData.get("name"));
            if (gData.has("description"))
                group.setDescription((String) gData.get("description"));

            em.getTransaction().begin();
            em.persist(group);
            em.getTransaction().commit();

            URI resourceUri = new URI(String.format("/%d",
                    group.getGroupId()));
            ResponseBuilder rb = Response.created(resourceUri);
            Response r = rb.build();
            return r;
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
    public IUGroupResource subResource(int id) throws Exception {
        UGroup g = UGroupDAO.findById(id);
        if (g == null) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
        else {
            return new UGroupResource(g);
        }
    }

}
