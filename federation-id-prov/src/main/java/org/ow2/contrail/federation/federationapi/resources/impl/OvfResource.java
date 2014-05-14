package org.ow2.contrail.federation.federationapi.resources.impl;

import org.apache.log4j.Logger;
import org.ow2.contrail.federation.federationapi.resources.IOvfResource;
import org.ow2.contrail.federation.federationapi.utils.JSONObject;
import org.ow2.contrail.federation.federationdb.jpa.entities.Ovf;
import org.ow2.contrail.federation.federationdb.jpa.entities.Provider;
import org.ow2.contrail.federation.federationdb.utils.PersistenceUtils;

import javax.persistence.EntityManager;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;

public class OvfResource implements IOvfResource {

    protected static Logger logger =
            Logger.getLogger(OvfResource.class);

    private Ovf ovf;
    private Provider provider;

    public OvfResource(Ovf ovf) {
        this.ovf = ovf;
    }

    public OvfResource(Provider provider, Ovf ovf) {
        this.ovf = ovf;
        this.provider = provider;
    }

    /**
     * Returns the JSON representation of the selected OVF.
     *
     * @return
     */
    @GET
    @Produces("application/json")
    public Response getOvf() throws Exception {
        logger.debug("Entering getOvf");
        JSONObject ovf = new JSONObject();
        ovf.put("providerId", String.format("/providers/%d", this.ovf.getProviderId().getProviderId()));
        ovf.put("ovfId", this.ovf.getOvfId());
        ovf.put("name", this.ovf.getName());
        ovf.put("attributes", this.ovf.getAttributes());
        logger.debug("Exiting getOvf");
        return Response.ok(ovf.toString()).build();
    }

    /**
     * Updates the selected OVF.
     *
     * @return
     */
    @PUT
    @Consumes("application/json")
    public Response updateOvf(String requestBody) throws Exception {
        logger.debug("Modifying ovf");
        JSONObject json = new JSONObject(requestBody);
        String name = (String) json.get("name");
        ovf.setName(name);
        ovf.setAttributes(json.toString());
        EntityManager em = PersistenceUtils.getInstance().getEntityManager();
        try {
            em.getTransaction().begin();
            ovf = em.merge(ovf);
            em.getTransaction().commit();
            return Response.status(Response.Status.NO_CONTENT).build();
        }
        finally {
            logger.debug("Exiting modifying ovf");
            PersistenceUtils.getInstance().closeEntityManager(em);
        }
    }

    /**
     * Deletes selected OVF.
     *
     * @return
     */
    @DELETE
    public Response removeOvf() throws Exception {
        EntityManager em = PersistenceUtils.getInstance().getEntityManager();
        try {
            em.getTransaction().begin();
            ovf = em.merge(ovf);
            em.remove(ovf);
            provider.getOvfList().remove(ovf);
            provider = em.merge(provider);
            em.getTransaction().commit();

            return Response.status(Response.Status.NO_CONTENT).build();
        }
        finally {
            PersistenceUtils.getInstance().closeEntityManager(em);
        }
    }
}
