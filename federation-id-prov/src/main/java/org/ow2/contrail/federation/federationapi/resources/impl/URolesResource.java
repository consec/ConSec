/**
 *
 */
package org.ow2.contrail.federation.federationapi.resources.impl;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.ow2.contrail.federation.federationapi.resources.IURoleResource;
import org.ow2.contrail.federation.federationapi.resources.IURolesResource;
import org.ow2.contrail.federation.federationapi.utils.FederationDBCommon;
import org.ow2.contrail.federation.federationdb.jpa.dao.URoleDAO;
import org.ow2.contrail.federation.federationdb.jpa.entities.URole;
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
@Path("/roles")
public class URolesResource implements IURolesResource {

    protected static Logger logger =
            Logger.getLogger(URolesResource.class);

    /* (non-Javadoc)
     * @see org.ow2.contrail.federation.federationapi.interfaces.BaseCollection#get()
     */
    @Override
    public Response get() throws Exception {
        EntityManager em = PersistenceUtils.getInstance().getEntityManager();
        try {
            Query query = em.createNamedQuery("URole.findAll");
            List<URole> roleList = query.getResultList();
            JSONArray UriList = new JSONArray();
            for (URole role : roleList) {
                String uri = String.format("/roles/%d", role.getRoleId());
                JSONObject o = new JSONObject();
                o.put("name", role.getName());
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
        JSONObject roleData = null;
        try {
            roleData = new JSONObject(content);
        }
        catch (Exception err) {
            logger.error(err.getMessage());
            logger.error(FederationDBCommon.getStackTrace(err));
            return Response.status(Response.Status.NOT_ACCEPTABLE).build();
        }
        String name = (String) roleData.get("name");
        EntityManager em = PersistenceUtils.getInstance().getEntityManager();
        try {
            Query query = em.createQuery("SELECT COUNT(u) FROM URole u WHERE u.name=:name");
            query.setParameter("name", name);
            if ((Long) query.getSingleResult() > 0) {
                // resource is already registered
                return Response.status(Response.Status.CONFLICT).build();
            }

            URole role = new URole();
            if (roleData.has("name"))
                role.setName(name);
            else {
                logger.error("Role does not have a name attribute.");
                return Response.status(Response.Status.NOT_ACCEPTABLE).build();
            }

            if (roleData.has("name"))
                role.setName((String) roleData.get("name"));
            if (roleData.has("description"))
                role.setDescription((String) roleData.get("description"));
            if (roleData.has("acl"))
                role.setDescription((String) roleData.get("acl"));

            em.getTransaction().begin();
            em.persist(role);
            em.getTransaction().commit();

            URI resourceUri = new URI(String.format("/%d",
                    role.getRoleId()));
            return Response.created(resourceUri).build();
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
    public IURoleResource subResource(int id) throws Exception {
        URole role = URoleDAO.findById(id);
        if (role == null) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
        else {
            return new URoleResource(role);
        }
    }

}
