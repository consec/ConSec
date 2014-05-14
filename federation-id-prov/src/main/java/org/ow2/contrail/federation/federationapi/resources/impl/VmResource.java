package org.ow2.contrail.federation.federationapi.resources.impl;

import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;
import org.ow2.contrail.federation.federationapi.resources.IVmResource;
import org.ow2.contrail.federation.federationdb.jpa.entities.Provider;
import org.ow2.contrail.federation.federationdb.jpa.entities.Vm;
import org.ow2.contrail.federation.federationdb.utils.PersistenceUtils;

import javax.persistence.EntityManager;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;

public class VmResource implements IVmResource {
    private static Logger log = Logger.getLogger(VmResource.class);
    private Provider provider;
    private Vm vm;
    private String rootUri;

    public VmResource(Provider provider, Vm vm) {
        this.provider = provider;
        this.vm = vm;
        rootUri = String.format("/providers/%d/vms/%d", provider.getProviderId(), vm.getVmId());
    }

    /**
     * Returns the JSON representation of the selected VM.
     *
     * @return
     */
    @GET
    @Produces("application/json")
    public Response getVm() throws JSONException {
        if (log.isTraceEnabled()) {
            log.trace(String.format("getVm(ID=%d) started.", vm.getVmId()));
        }

        JSONObject json = vm.toJSON();
        json.put("uri", rootUri);

        return Response.ok(json.toString()).build();
    }

    /**
     * Updates the selected VM.
     *
     * @return
     */
    @PUT
    @Consumes("application/json")
    public Response updateVm(String requestBody) throws Exception {
        if (log.isTraceEnabled()) {
            log.trace(String.format("updateVm(ID=%d) started. Data: %s", vm.getVmId(), requestBody));
        }

        EntityManager em = PersistenceUtils.getInstance().getEntityManager();
        try {
            JSONObject json = new JSONObject(requestBody);
            vm.update(json);

            em.getTransaction().begin();
            em.merge(vm);
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
     * Deletes selected VM.
     *
     * @return
     */
    @DELETE
    public Response removeVm() throws Exception {
        if (log.isTraceEnabled()) {
            log.trace(String.format("removeVm(ID=%d) started.", vm.getVmId()));
        }

        EntityManager em = PersistenceUtils.getInstance().getEntityManager();
        try {
            em.getTransaction().begin();
            vm = em.merge(vm);
            em.remove(vm);
            provider.getVmList().remove(vm);
            provider = em.merge(provider);
            em.getTransaction().commit();
            return Response.status(Response.Status.NO_CONTENT).build();
        }
        finally {
            PersistenceUtils.getInstance().closeEntityManager(em);
        }
    }

    /**
     * Returns the VMEx.
     *
     * @return
     */
    @GET
    @Path("/vmex")
    @Produces("application/json")
    public Response getVmex() {
        String json = String.format("Provider %d VM %d vmex.", provider.getProviderId(), vm.getVmId());
        return Response.ok(json).build();
    }

    /**
     * Updates the VMEx.
     *
     * @return
     */
    @PUT
    @Path("/vmex")
    public Response updateVmex() throws Exception {
        return Response.ok().build();
    }

    /**
     * Returns the vmslot.
     *
     * @return
     */
    @GET
    @Path("/vmslot")
    @Produces("application/json")
    public Response getVmslot() {
        String json = String.format("Provider %d VM %d vmslot.", provider.getProviderId(), vm.getVmId());
        return Response.ok(json).build();
    }

    /**
     * Updates the vmslot.
     *
     * @return
     */
    @PUT
    @Path("/vmslot")
    public Response updateVmslot() throws Exception {
        return Response.ok().build();
    }

    /**
     * Returns the vmapp.
     *
     * @return
     */
    @GET
    @Path("/vmapp")
    @Produces("application/json")
    public Response getVmapp() {
        String json = String.format("Provider %d VM %d vmapp.", provider.getProviderId(), vm.getVmId());
        return Response.ok(json).build();
    }

    /**
     * Updates the vmapp.
     *
     * @return
     */
    @PUT
    @Path("/vmapp")
    public Response updateVmapp() throws Exception {
        return Response.ok().build();
    }
}
