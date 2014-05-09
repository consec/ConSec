/**
 *
 */
package org.ow2.contrail.federation.federationapi.resources;

import org.apache.log4j.Logger;
import org.consec.federationdb.model.Group;
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

/**
 * @author ales
 */
@Path("/groups")
public class UserGroupsResource {

    protected static Logger logger =
            Logger.getLogger(UserGroupsResource.class);

    @GET
    @Produces("application/json")
    public Response get() throws Exception {
        EntityManager em = EMF.createEntityManager();
        try {
            Query query = em.createNamedQuery("Group.findAll");
            List<Group> gList = query.getResultList();
            JSONArray uriList = new JSONArray();
            for (Group g : gList) {
                JSONObject o = new JSONObject();
                o.put("name", g.getName());
                o.put("uri", RestUriBuilder.getGroupUri(g));
                uriList.put(o);
            }
            return Response.ok(uriList.toString()).build();
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
    public Response post(JSONObject groupData) throws Exception {
        logger.debug("Entering post");

        String name = (String) groupData.get("name");
        EntityManager em = EMF.createEntityManager();
        try {
            Query query = em.createQuery("SELECT COUNT(u) FROM UGroup u WHERE u.name=:name");
            query.setParameter("name", name);
            if ((Long) query.getSingleResult() > 0) {
                // resource is already registered
                return Response.status(Response.Status.CONFLICT).build();
            }

            Group group = new Group();
            if (groupData.has("name"))
                group.setName(name);
            else {
                logger.error("Group does not have a name attribute.");
                return Response.status(Response.Status.NOT_ACCEPTABLE).build();
            }

            if (groupData.has("name"))
                group.setName((String) groupData.get("name"));
            if (groupData.has("description"))
                group.setDescription((String) groupData.get("description"));

            em.getTransaction().begin();
            em.persist(group);
            em.getTransaction().commit();

            URI resourceUri = new URI(String.format("/%d", group.getGroupId()));
            return Response.created(resourceUri).build();
        }
        finally {
            EMF.closeEntityManager(em);
            logger.debug("Exiting post");
        }
    }
}
