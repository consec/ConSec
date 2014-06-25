package org.consec.dynamicca.rest;

import org.bouncycastle.cert.X509CRLHolder;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.consec.dynamicca.jpa.EMF;
import org.consec.dynamicca.jpa.entities.Ca;
import org.consec.dynamicca.jpa.entities.Cert;
import org.consec.dynamicca.utils.CertUtils;

import javax.persistence.EntityManager;
import javax.ws.rs.*;
import javax.ws.rs.core.*;

@Path("/cas/{caUid}")
public class CaResource {
    private String caUid;

    @Context
    UriInfo uriInfo;

    public CaResource(@PathParam("caUid") String caUid) {
        this.caUid = caUid;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public JSONObject getCa() throws JSONException {
        EntityManager em = EMF.createEntityManager();
        try {
            Ca ca = em.find(Ca.class, caUid);
            if (ca == null) {
                throw new WebApplicationException(Response.Status.NOT_FOUND);
            }

            JSONObject o = new JSONObject();
            o.put("uid", ca.getUid());
            o.put("name", ca.getName());
            o.put("uri", uriInfo.getAbsolutePath());
            o.put("revocation_list", uriInfo.getAbsolutePathBuilder().path("crl").build());
            o.put("cacert", uriInfo.getAbsolutePathBuilder().path("cacert").build());
            return o;
        }
        finally {
            EMF.closeEntityManager(em);
        }
    }

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/cacert")
    public String getCaCert() throws JSONException {
        EntityManager em = EMF.createEntityManager();
        try {
            Ca ca = em.find(Ca.class, caUid);
            if (ca == null) {
                throw new WebApplicationException(Response.Status.NOT_FOUND);
            }

            return ca.getCertificate();
        }
        finally {
            EMF.closeEntityManager(em);
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("certs")
    public JSONObject generateCert(JSONObject data) throws Exception {
        EntityManager em = EMF.createEntityManager();
        try {
            Ca ca = em.find(Ca.class, caUid);
            if (ca == null) {
                throw new WebApplicationException(Response.Status.NOT_FOUND);
            }

            String userUuid;
            try {
                userUuid = data.getString("user_uuid");
            }
            catch (Exception e) {
                throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                        .entity("Bad request: " + e.getMessage()).build());
            }

            CertUtils certUtils = new CertUtils(em);
            Cert userCert = certUtils.createUserCertificate(userUuid, ca);

            em.getTransaction().begin();
            em.persist(userCert);
            em.getTransaction().commit();

            JSONObject result = new JSONObject();
            result.put("private_key", userCert.getPrivateKey());
            result.put("certificate", userCert.getCertificate());
            result.put("serial_number", userCert.getCertPK().getSn());
            result.put("uri", UriBuilder.fromResource(CertResource.class)
                    .build(ca.getUid(), userCert.getCertPK().getSn()));
            return result;
        }
        finally {
            EMF.closeEntityManager(em);
        }
    }

    @GET
    @Produces("application/pkix-crl")
    @Path("crl")
    public byte[] getCertRevocationList() throws Exception {
        EntityManager em = EMF.createEntityManager();
        try {
            Ca ca = em.find(Ca.class, caUid);
            if (ca == null) {
                throw new WebApplicationException(Response.Status.NOT_FOUND);
            }

            CertUtils certUtils = new CertUtils(em);
            X509CRLHolder crlHolder = certUtils.createCRL(ca);

            return crlHolder.getEncoded();
        }
        finally {
            EMF.closeEntityManager(em);
        }
    }
}
