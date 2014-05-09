package org.ow2.contrail.federation.federationapi.resources;

import org.consec.federationdb.model.Attribute;
import org.consec.federationdb.model.IdentityProvider;
import org.consec.federationdb.utils.EMF;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.persistence.EntityManager;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.util.Iterator;

@Path("/idps/{idpUuid}/attributes")
public class IdpAttributesResource {
    private String idpUuid;

    public IdpAttributesResource(@PathParam("idpUuid") String idpUuid) {
        this.idpUuid = idpUuid;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAttributes() throws Exception {
        EntityManager em = EMF.createEntityManager();

        try {
            IdentityProvider idp = em.find(IdentityProvider.class, idpUuid);
            if (idp == null) {
                throw new WebApplicationException(Response.Status.NOT_FOUND);
            }

            JSONObject attributes = new JSONObject(idp.getAttributes());
            Iterator it = attributes.keys();
            JSONArray result = new JSONArray();
            while (it.hasNext()) {
                String attrUuid = (String) it.next();
                String attrValue = attributes.getString(attrUuid);
                Attribute attr = em.find(Attribute.class, attrUuid);
                if (attr == null) {
                    throw new Exception(String.format(
                            "Invalid attribute with UUID '%s' found at IdP %s.", attrUuid, idp.getIdpId()));
                }

                JSONObject attrData = new JSONObject();
                attrData.put("name", attr.getName());
                attrData.put("value", attrValue);
                attrData.put("uri",
                        UriBuilder.fromResource(IdpAttributesResource.class).path(attrUuid).build(idpUuid));
                result.put(attrData);
            }

            return Response.ok(result.toString()).build();
        }
        finally {
            EMF.closeEntityManager(em);
        }
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateAttributes(String data) throws Exception {
        EntityManager em = EMF.createEntityManager();
        try {
            JSONArray attrDataArray = new JSONArray(data);

            IdentityProvider idp = em.find(IdentityProvider.class, idpUuid);
            if (idp == null) {
                throw new WebApplicationException(Response.Status.NOT_FOUND);
            }
            JSONObject attributes = new JSONObject(idp.getAttributes());

            for (int i = 0; i < attrDataArray.length(); i++) {
                JSONObject o = attrDataArray.getJSONObject(i);
                String attrUuid = o.getString("uuid");
                String attrValue = o.getString("value");
                Attribute attr = em.find(Attribute.class, attrUuid);
                if (attr == null) {
                    throw new JSONException(String.format("Invalid attribute uuid '%s'.", attrUuid));
                }
                attributes.put(attrUuid, attrValue);
            }

            em.getTransaction().begin();
            idp.setAttributes(attributes.toString());
            em.getTransaction().commit();

            return Response.noContent().build();
        }
        catch (JSONException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Invalid JSON data: " + e.getMessage()).build();
        }
        finally {
            EMF.closeEntityManager(em);
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{attrUuid:[\\w-]+}")
    public Response getAttribute(@PathParam("attrUuid") String attrUuid) throws Exception {
        EntityManager em = EMF.createEntityManager();

        try {
            IdentityProvider idp = em.find(IdentityProvider.class, idpUuid);
            if (idp == null) {
                throw new WebApplicationException(Response.Status.NOT_FOUND);
            }

            JSONObject attributes = new JSONObject(idp.getAttributes());
            if (!attributes.has(attrUuid)) {
                throw new WebApplicationException(Response.Status.NOT_FOUND);
            }
            String attrValue = attributes.getString(attrUuid);

            Attribute attr = em.find(Attribute.class, attrUuid);
            if (attr == null) {
                throw new Exception(String.format(
                        "Invalid attribute with UUID '%s' found at IdP %s.", attrUuid, idp.getIdpId()));
            }

            JSONObject attrData = new JSONObject();
            attrData.put("name", attr.getName());
            attrData.put("value", attrValue);
            attrData.put("uri",
                    UriBuilder.fromResource(IdpAttributesResource.class).path(attrUuid).build(idpUuid));

            return Response.ok(attrData.toString()).build();
        }
        finally {
            EMF.closeEntityManager(em);
        }
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("{attrUuid:[\\w-]+}")
    public Response updateAttribute(@PathParam("attrUuid") String attrUuid, String data) throws Exception {
        EntityManager em = EMF.createEntityManager();

        try {
            IdentityProvider idp = em.find(IdentityProvider.class, idpUuid);
            if (idp == null) {
                throw new WebApplicationException(Response.Status.NOT_FOUND);
            }

            JSONObject attributes = new JSONObject(idp.getAttributes());
            if (!attributes.has(attrUuid)) {
                throw new WebApplicationException(Response.Status.NOT_FOUND);
            }

            JSONObject o = new JSONObject(data);
            String attrValue = o.getString("value");
            attributes.put(attrUuid, attrValue);

            em.getTransaction().begin();
            idp.setAttributes(attributes.toString());
            em.getTransaction().commit();

            return Response.status(Response.Status.NO_CONTENT).build();
        }
        catch (JSONException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Invalid JSON data: " + e.getMessage()).build();
        }
        finally {
            EMF.closeEntityManager(em);
        }
    }

    @DELETE
    @Path("{attrUuid:[\\w-]+}")
    public Response removeAttribute(@PathParam("attrUuid") String attrUuid) throws Exception {
        EntityManager em = EMF.createEntityManager();

        try {
            IdentityProvider idp = em.find(IdentityProvider.class, idpUuid);
            if (idp == null) {
                throw new WebApplicationException(Response.Status.NOT_FOUND);
            }

            JSONObject attributes = new JSONObject(idp.getAttributes());
            if (!attributes.has(attrUuid)) {
                throw new WebApplicationException(Response.Status.NOT_FOUND);
            }

            attributes.remove(attrUuid);

            em.getTransaction().begin();
            idp.setAttributes(attributes.toString());
            em.getTransaction().commit();

            return Response.status(Response.Status.NO_CONTENT).build();
        }
        finally {
            EMF.closeEntityManager(em);
        }
    }
}
