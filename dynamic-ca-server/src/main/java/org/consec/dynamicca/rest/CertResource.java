package org.consec.dynamicca.rest;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.consec.dynamicca.jpa.EMF;
import org.consec.dynamicca.jpa.entities.Cert;
import org.consec.dynamicca.jpa.entities.CertPK;

import javax.persistence.EntityManager;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.util.Date;

@Path("/cas/{caUid}/certs/{sn}")
public class CertResource {
    private String caUid;
    private int sn;

    @Context
    UriInfo uriInfo;

    public CertResource(@PathParam("caUid") String caUid,
                        @PathParam("sn") int sn) {
        this.caUid = caUid;
        this.sn = sn;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public JSONObject getCert() throws JSONException {
        EntityManager em = EMF.createEntityManager();
        try {
            CertPK certPK = new CertPK(sn, caUid);
            Cert cert = em.find(Cert.class, certPK);
            if (cert == null) {
                throw new WebApplicationException(Response.Status.NOT_FOUND);
            }

            JSONObject result = new JSONObject();
            result.put("private_key", cert.getPrivateKey());
            result.put("certificate", cert.getCertificate());
            result.put("serial_number", cert.getCertPK().getSn());
            result.put("uri", UriBuilder.fromResource(CertResource.class)
                    .build(cert.getCa().getUid(), cert.getCertPK().getSn()));
            return result;
        }
        finally {
            EMF.closeEntityManager(em);
        }
    }

    @DELETE
    public Response revokeCert() {
        EntityManager em = EMF.createEntityManager();
        try {
            CertPK certPK = new CertPK(sn, caUid);
            Cert cert = em.find(Cert.class, certPK);
            if (cert == null) {
                throw new WebApplicationException(Response.Status.NOT_FOUND);
            }

            if (cert.getRevoked()) {
                throw new WebApplicationException(Response.Status.NOT_MODIFIED);
            }

            em.getTransaction().begin();
            cert.setRevoked(true);
            cert.setRevocationDate(new Date());
            em.getTransaction().commit();

            String message = String.format("The certificate with serial number %d has been revoked successfully.", sn);
            return Response.status(Response.Status.NO_CONTENT).entity(message).build();
        }
        finally {
            EMF.closeEntityManager(em);
        }
    }
}