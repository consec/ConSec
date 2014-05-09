/**
 *
 */
package org.ow2.contrail.federation.federationapi.resources;

import org.apache.log4j.Logger;
import org.consec.federationdb.model.Role;
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
@Path("/roles")
public class UserRolesResource {

    protected static Logger logger = Logger.getLogger(UserRolesResource.class);

    @GET
    @Produces("application/json")
    public Response get() throws Exception {
        EntityManager em = EMF.createEntityManager();
        try {
            Query query = em.createNamedQuery("Role.findAll");
            List<Role> roleList = query.getResultList();
            JSONArray uriList = new JSONArray();
            for (Role role : roleList) {
                JSONObject o = new JSONObject();
                o.put("name", role.getName());
                o.put("uri", RestUriBuilder.getRoleUri(role));
                uriList.put(o);
            }
            return Response.ok(uriList.toString()).build();
        }
        finally {
            EMF.closeEntityManager(em);
        }
    }

    @POST
    @Consumes("application/json")
    @Produces("application/json")
    public Response post(JSONObject roleData) throws Exception {
        EntityManager em = EMF.createEntityManager();
        try {
            Role role = new Role();

            if (roleData.has("name"))
                role.setName((String) roleData.get("name"));
            if (roleData.has("description"))
                role.setDescription((String) roleData.get("description"));
            if (roleData.has("acl"))
                role.setDescription((String) roleData.get("acl"));

            em.getTransaction().begin();
            em.persist(role);
            em.getTransaction().commit();

            URI resourceUri = new URI(String.format("/%d", role.getRoleId()));
            return Response.created(resourceUri).build();
        }
        finally {
            EMF.closeEntityManager(em);
            logger.debug("Exiting post");
        }
    }
}
