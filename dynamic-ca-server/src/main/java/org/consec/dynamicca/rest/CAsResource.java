package org.consec.dynamicca.rest;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.consec.dynamicca.jpa.EMF;
import org.consec.dynamicca.jpa.entities.Ca;
import org.consec.dynamicca.utils.CertUtils;
import org.consec.dynamicca.utils.DBUtils;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.TypedQuery;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.UUID;

@Path("/cas")
public class CAsResource {
    protected static Logger log = Logger.getLogger(CAsResource.class);

    @Context
    UriInfo uriInfo;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public JSONArray getCAs() throws URISyntaxException {
        EntityManager em = EMF.createEntityManager();
        try {
            TypedQuery<Ca> query = em.createNamedQuery("Ca.findAll", Ca.class);
            List<Ca> caList = query.getResultList();
            JSONArray jsonArray = new JSONArray();
            for (Ca ca : caList) {
                UriBuilder ub = uriInfo.getAbsolutePathBuilder();
                URI caUri = ub.path(ca.getUid()).build();
                jsonArray.put(caUri);
            }
            return jsonArray;
        }
        finally {
            EMF.closeEntityManager(em);
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response addCA(JSONObject data) throws Exception {
        String name;
        try {
            name = data.getString("name");
        }
        catch (JSONException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
        }

        EntityManager em = EMF.createEntityManager();
        try {
            String caUid = UUID.randomUUID().toString();
            Ca ca = new Ca();
            ca.setUid(caUid);
            ca.setName(name);

            EntityTransaction t = em.getTransaction();
            t.begin();
            em.persist(ca);
            try {
                CertUtils certUtils = new CertUtils(em);
                certUtils.createCACertificate(ca);
                t.commit();
            }
            catch (Exception e) {
                if (t.isActive()) {
                    em.getTransaction().rollback();
                }
                if (DBUtils.isIntegrityConstraintException(e)) {
                    return Response.status(Response.Status.CONFLICT).build();
                }
                log.error("Failed to create CA certificate: " + e.getMessage(), e);
                throw new Exception("Failed to create CA certificate: " + e.getMessage());
            }

            JSONObject o = new JSONObject();
            o.put("uid", ca.getUid());

            URI location = new URI(ca.getUid());
            return Response.created(location).entity(o.toString()).build();
        }

        finally

        {
            EMF.closeEntityManager(em);
        }
    }
}
