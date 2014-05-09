/**
 *
 */
package org.ow2.contrail.federation.federationapi.resources;

import org.apache.log4j.Logger;
import org.consec.federationdb.model.Group;
import org.consec.federationdb.model.User;
import org.consec.federationdb.utils.EMF;
import org.json.JSONArray;
import org.ow2.contrail.federation.federationapi.utils.JSONObject;
import org.ow2.contrail.federation.federationapi.utils.RestUriBuilder;

import javax.persistence.EntityManager;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;

/**
 * @author ales
 */
@Path("/groups/{groupId}")
public class UserGroupResource {

    protected static Logger logger = Logger.getLogger(UserGroupResource.class);
    private int groupId;

    public UserGroupResource(@PathParam("groupId") int groupId) {
        this.groupId = groupId;
    }

    @GET
    @Produces("application/json")
    public Response get() throws Exception {
        EntityManager em = EMF.createEntityManager();
        try {
            Group group = em.find(Group.class, groupId);
            if (group == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }

            String baseUri = RestUriBuilder.getGroupUri(group);
            JSONObject json = new JSONObject();
            json.put("groupId", group.getGroupId());
            json.put("name", group.getName());
            json.put("description", group.getDescription());
            json.put("users", baseUri + "/users");
            return Response.ok(json.toString()).build();
        }
        finally {
            EMF.closeEntityManager(em);
        }
    }

    /**
     * @return the list of users who are in this group.
     * @throws Exception
     */
    @GET
    @Produces("application/json")
    @Path("/users")
    public Response getUsers() throws Exception {
        EntityManager em = EMF.createEntityManager();
        try {
            Group group = em.find(Group.class, groupId);
            if (group == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }

            JSONArray usersArr = new JSONArray();
            for (User user : group.getUserList()) {
                usersArr.put(RestUriBuilder.getUserUri(user));
            }
            return Response.ok(usersArr.toString()).build();
        }
        finally {
            EMF.closeEntityManager(em);
        }
    }

    @DELETE
    public Response delete() throws Exception {
        EntityManager em = EMF.createEntityManager();
        try {
            Group group = em.find(Group.class, groupId);
            if (group == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }

            em.getTransaction().begin();
            em.remove(group);
            em.getTransaction().commit();

            return Response.status(Response.Status.NO_CONTENT).build();
        }
        finally {
            EMF.closeEntityManager(em);
        }
    }

    @PUT
    @Consumes("application/json")
    @Produces("application/json")
    public Response put(JSONObject gData) throws Exception {
        logger.debug("Entering put");

        EntityManager em = EMF.createEntityManager();
        try {
            Group group = em.find(Group.class, groupId);
            if (group == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }

            em.getTransaction().begin();
            if (gData.has("name"))
                group.setName(gData.getString("name"));
            if (gData.has("description"))
                group.setDescription(gData.getString("description"));
            em.getTransaction().commit();

            return Response.status(Response.Status.NO_CONTENT).build();
        }
        finally {
            EMF.closeEntityManager(em);
            logger.debug("Exiting put");
        }
    }
}
