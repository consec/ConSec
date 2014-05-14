package org.ow2.contrail.federation.federationapi.resources.impl;

import org.ow2.contrail.federation.federationapi.resources.ICEEResource;

import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

public class CEEResource implements ICEEResource {

    private int providerId;
    private int voId;
    private int ceeId;

    public CEEResource(int providerId, int voId, int ceeId) {
        this.providerId = providerId;
        this.voId = voId;
        this.ceeId = ceeId;
    }

    /**
     * Returns the JSON representation of the given CEE.
     *
     * @return
     */
    @GET
    @Produces("application/json")
    public Response getCEE() {
        String json = String.format("Provider %d VO %d CEE %d", providerId, voId, ceeId);
        return Response.ok(json).build();
    }

    /**
     * Updates the given CEE.
     *
     * @return
     */
    @PUT
    public Response updateCEE() {
        return Response.ok().build();
    }
}
