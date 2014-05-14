package org.ow2.contrail.federation.federationapi.resources.impl;

import org.ow2.contrail.federation.federationapi.resources.INetworkResource;

import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

public class NetworkResource implements INetworkResource {
    private int providerId;
    private int voId;
    private int edcId;
    private int networkId;

    /**
     * Creates network resource for the elastic data center of the provider's virtual organization
     *
     * @param providerId
     * @param voId
     * @param edcId
     * @param networkId
     */
    public NetworkResource(int providerId, int voId, int edcId, int networkId) {
        this.providerId = providerId;
        this.voId = voId;
        this.edcId = edcId;
        this.networkId = networkId;
    }

    /**
     * Creates network resource for the provider's data center.
     *
     * @param providerId
     * @param dcId
     * @param networkId
     */
    public NetworkResource(int providerId, int dcId, int networkId) {
        this.providerId = providerId;
        this.networkId = networkId;
    }

    public NetworkResource(int providerId, int networkId) {
        this.providerId = providerId;
        this.networkId = networkId;
    }

    /**
     * Returns the JSON representation of the given network.
     *
     * @return
     */
    @GET
    @Produces("application/json")
    public Response getNetwork() {
        String json = String.format("Provider %d VO %d EDC %d network %d",
                providerId, voId, edcId, networkId);
        return Response.ok(json).build();
    }

    /**
     * Updates the given network.
     *
     * @return
     */
    @PUT
    public Response updateNetwork() {
        return Response.ok().build();
    }
}
