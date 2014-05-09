package org.ow2.contrail.federation.federationapi.resources;

import org.apache.log4j.Logger;
import org.consec.federationdb.model.Role;
import org.consec.federationdb.model.User;
import org.consec.federationdb.utils.EMF;
import org.json.JSONArray;
import org.ow2.contrail.federation.federationapi.utils.JSONObject;
import org.ow2.contrail.federation.federationapi.utils.RestUriBuilder;

import javax.persistence.EntityManager;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;

@Path("/roles/{roleId}")
public class UserRoleResource {

    protected static Logger logger = Logger.getLogger(UserRoleResource.class);

    private int roleId;

    public UserRoleResource(@PathParam("roleId") int roleId) {
        this.roleId = roleId;
    }

    @GET
    @Produces("application/json")
    public Response get() throws Exception {
        EntityManager em = EMF.createEntityManager();
        try {
            Role role = em.find(Role.class, roleId);
            if (role == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }

            String baseUri = RestUriBuilder.getRoleUri(role);
            JSONObject attr = new JSONObject();
            attr.put("roleId", role.getRoleId());
            attr.put("name", role.getName());
            attr.put("description", role.getDescription());
            attr.put("acl", role.getAcl());
            attr.put("users", baseUri + "/users");
            return Response.ok(attr.toString()).build();
        }
        finally {
            EMF.closeEntityManager(em);
        }
    }

    @GET
    @Produces("application/json")
    @Path("/users")
    public Response getUsers() throws Exception {
        EntityManager em = EMF.createEntityManager();
        try {
            Role role = em.find(Role.class, roleId);
            if (role == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }

            JSONArray attr = new JSONArray();
            for (User user : role.getUserList()) {
                attr.put(RestUriBuilder.getUserUri(user));
            }
            return Response.ok(attr.toString()).build();
        }
        finally {
            EMF.closeEntityManager(em);
        }
    }

    @DELETE
    public Response delete() throws Exception {
        EntityManager em = EMF.createEntityManager();
        try {
            Role role = em.find(Role.class, roleId);
            if (role == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }

            em.getTransaction().begin();
            em.remove(role);
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
    public Response put(JSONObject roleData) throws Exception {
        logger.debug("Entering put");

        EntityManager em = EMF.createEntityManager();
        try {
            Role role = em.find(Role.class, roleId);
            if (role == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }

            em.getTransaction().begin();

            if (roleData.has("name"))
                role.setName(roleData.getString("name"));
            if (roleData.has("description"))
                role.setDescription(roleData.getString("description"));
            if (roleData.has("acl"))
                role.setAcl(roleData.getString("acl"));

            em.getTransaction().commit();

            return Response.status(Response.Status.NO_CONTENT).build();
        }
        finally {
            EMF.closeEntityManager(em);
            logger.debug("Exiting put");
        }
    }
}
