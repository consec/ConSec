package org.ow2.contrail.federation.federationapi.resources.impl;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.ow2.contrail.federation.federationapi.resources.ISLATemplateResource;
import org.ow2.contrail.federation.federationdb.jpa.entities.Provider;
import org.ow2.contrail.federation.federationdb.jpa.entities.SLATemplate;
import org.ow2.contrail.federation.federationdb.jpa.entities.UserSLATemplate;
import org.ow2.contrail.federation.federationdb.utils.PersistenceUtils;

import javax.persistence.EntityManager;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;

public class SLATemplateResource implements ISLATemplateResource {
    private static Logger log = Logger.getLogger(SLATemplateResource.class);
    private Provider provider;
    private SLATemplate slaTemplate;
    private String rootUri;

    public SLATemplateResource(Provider provider, SLATemplate slaTemplate) {
        this.provider = provider;
        this.slaTemplate = slaTemplate;
        rootUri = String.format("/providers/%d/slats/%d", provider.getProviderId(), slaTemplate.getSlatId());
    }

    /**
     * Returns the JSON representation of the given SLA template.
     *
     * @return
     */
    @Override
    @GET
    @Produces("application/json")
    public Response getSLATemplate() throws Exception {
        if (log.isTraceEnabled()) {
            log.trace(String.format("getSLATemplate(ID=%d) started.", slaTemplate.getSlatId()));
        }

        JSONObject json = slaTemplate.toJSON();
        json.put("uri", rootUri);

        JSONArray arr = new JSONArray();
        for (UserSLATemplate userSlat : this.slaTemplate.getUserSLATemplateList()) {
            arr.put(String.format("/users/%d/slas/%d",
                    userSlat.getUserId().getUserId(),
                    userSlat.getSLATemplateId()));
        }
        json.put("userSLATs", arr);

        return Response.ok(json.toString()).build();
    }

    /**
     * Updates the selected SLA template.
     *
     * @return
     */
    @Override
    @PUT
    @Consumes("application/json")
    public Response updateSLATemplate(String requestBody) throws Exception {
        if (log.isTraceEnabled()) {
            log.trace(String.format("updateSLATemplate(ID=%d) started. Data: %s", slaTemplate.getSlatId(), requestBody));
        }

        EntityManager em = PersistenceUtils.getInstance().getEntityManager();
        try {
            JSONObject json = new JSONObject(requestBody);
            slaTemplate.update(json);

            em.getTransaction().begin();
            slaTemplate = em.merge(slaTemplate);
            em.getTransaction().commit();
            return Response.status(Response.Status.NO_CONTENT).build();
        }
        catch (Exception e) {
            log.error("Update failed: ", e);
            throw new WebApplicationException(
                    Response.status(Response.Status.BAD_REQUEST).
                            entity(String.format("Update failed: %s.", e.getMessage())).
                            build()
            );
        }
        finally {
            PersistenceUtils.getInstance().closeEntityManager(em);
        }
    }

    /**
     * Unregisters selected SLA template.
     *
     * @return
     */
    @Override
    @DELETE
    public Response removeSLATemplate() throws Exception {
        if (log.isTraceEnabled()) {
            log.trace(String.format("removeSLATemplate(ID=%d) started.", slaTemplate.getSlatId()));
        }

        EntityManager em = PersistenceUtils.getInstance().getEntityManager();
        try {
            em.getTransaction().begin();
            slaTemplate = em.merge(slaTemplate);
            em.remove(slaTemplate);
            provider.getSLATemplateList().remove(slaTemplate);
            provider = em.merge(provider);
            em.getTransaction().commit();
            return Response.status(Response.Status.NO_CONTENT).build();
        }
        finally {
            PersistenceUtils.getInstance().closeEntityManager(em);
        }
    }
}
