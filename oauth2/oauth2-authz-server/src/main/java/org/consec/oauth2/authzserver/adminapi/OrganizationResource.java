package org.consec.oauth2.authzserver.adminapi;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.consec.oauth2.authzserver.jpa.entities.Organization;
import org.consec.oauth2.authzserver.utils.PersistenceUtils;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

@Path("/organizations")
public class OrganizationResource {

    @Context
    UriInfo uriInfo;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public JSONArray getOrganizations() throws URISyntaxException, JSONException {

        EntityManager em = PersistenceUtils.getInstance().getEntityManager();
        try {
            TypedQuery<Organization> q = em.createNamedQuery("Organization.findAll", Organization.class);
            List<Organization> organizations = q.getResultList();

            JSONArray jsonArray = new JSONArray();
            for (Organization organization : organizations) {
                JSONObject o = new JSONObject();
                o.put("uri", UriBuilder.fromResource(OrganizationResource.class)
                        .path("{id}").build(organization.getId()));
                o.put("id", organization.getId());
                o.put("name", organization.getName());

                jsonArray.put(o);
            }
            return jsonArray;
        }
        finally {
            PersistenceUtils.getInstance().closeEntityManager(em);
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response addOrganization(JSONObject data) throws Exception {

        Organization organization;
        try {
            String name = data.getString("name");

            organization = new Organization();
            organization.setName(name);
        }
        catch (Exception e) {
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                    .entity(e.getMessage()).build());
        }

        EntityManager em = PersistenceUtils.getInstance().getEntityManager();
        try {
            em.getTransaction().begin();
            em.persist(organization);
            em.getTransaction().commit();

            URI location = new URI(organization.getId().toString());
            return Response.created(location).build();
        }
        finally {
            PersistenceUtils.getInstance().closeEntityManager(em);
        }
    }

    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("{id}")
    public JSONObject getOrganization(@PathParam("id") int id) throws URISyntaxException, JSONException {

        EntityManager em = PersistenceUtils.getInstance().getEntityManager();
        try {
            Organization organization = em.find(Organization.class, id);
            if (organization == null) {
                throw new WebApplicationException(Response.Status.NOT_FOUND);
            }

            JSONObject o = new JSONObject();
            URI uri = UriBuilder.fromResource(OrganizationResource.class)
                    .path("{id}").build(organization.getId());
            o.put("uri", uri);
            o.put("id", organization.getId());
            o.put("name", organization.getName());
            o.put("clients", uri.toString() + "/clients");

            return o;
        }
        finally {
            PersistenceUtils.getInstance().closeEntityManager(em);
        }
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("{id}")
    public Response updateOrganization(@PathParam("id") int id, JSONObject data) throws Exception {

        EntityManager em = PersistenceUtils.getInstance().getEntityManager();
        try {
            Organization organization = em.find(Organization.class, id);
            if (organization == null) {
                throw new WebApplicationException(Response.Status.NOT_FOUND);
            }

            try {
                if (data.has("name")) {
                    organization.setName(data.getString("name"));
                }
            }
            catch (Exception e) {
                throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                        .entity(e.getMessage()).build());
            }

            em.getTransaction().begin();
            em.merge(organization);
            em.getTransaction().commit();

            return Response.noContent().build();
        }
        finally {
            PersistenceUtils.getInstance().closeEntityManager(em);
        }
    }

    @DELETE
    @Path("{id}")
    public Response deleteOrganization(@PathParam("id") int id) throws URISyntaxException, JSONException {

        EntityManager em = PersistenceUtils.getInstance().getEntityManager();
        try {
            Organization organization = em.find(Organization.class, id);
            if (organization == null) {
                throw new WebApplicationException(Response.Status.NOT_FOUND);
            }

            em.getTransaction().begin();
            em.remove(organization);
            em.getTransaction().commit();

            return Response.noContent().build();
        }
        finally {
            PersistenceUtils.getInstance().closeEntityManager(em);
        }
    }
}
