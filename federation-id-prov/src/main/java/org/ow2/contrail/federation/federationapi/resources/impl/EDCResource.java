package org.ow2.contrail.federation.federationapi.resources.impl;

import org.ow2.contrail.federation.federationapi.resources.IEDCResource;
import org.ow2.contrail.federation.federationapi.resources.INetworkResource;
import org.ow2.contrail.federation.federationapi.resources.IStorageResource;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.net.URI;

public class EDCResource implements IEDCResource {

    private int providerId;
    private int voId;
    private int edcId;

    public EDCResource(int providerId, int voId, int edcId) {
        this.providerId = providerId;
        this.voId = voId;
        this.edcId = edcId;
    }

    /**
     * Returns the JSON representation of the elastic DC for the provider's VO.
     *
     * @return
     */
    @GET
    @Produces("application/json")
    public Response getEDC() {
        String json = String.format("Provider %d VO %d EDC %d", providerId, voId, edcId);
        return Response.ok(json).build();
    }

    /**
     * Updates the provider’s elastic DC with the given ID.
     *
     * @return
     */
    @PUT
    public Response updateEDC() {
        return Response.ok().build();
    }

    /**
     * Returns all storages of the EDC.
     *
     * @return
     */
    @GET
    @Path("/storages")
    @Produces("application/json")
    public Response getStorages() {
        String json = String.format("Provider %d VO %d EDC %d storages.", providerId, voId, edcId);
        return Response.ok(json).build();
    }

    /**
     * Creates a new storage for the provider's EDC.
     *
     * @return
     */
    @POST
    @Path("/storages")
    public Response createStorage() throws Exception {
        URI resourceUri = new URI("/1");
        return Response.created(resourceUri).build();
    }

    /**
     * Sub-resource locator method. Returns the sub-resource object that can handle the remainder
     * of the request.
     *
     * @param storageId
     * @return
     */
    @Path("/storages/{sid}")
    public IStorageResource findStorage(@PathParam("sid") int storageId) {
        return new StorageResource(providerId, voId, edcId, storageId);
    }

    /**
     * Returns all networks of the EDC.
     *
     * @return
     */
    @GET
    @Path("/networks")
    @Produces("application/json")
    public Response getNetworks() {
        String json = String.format("Provider %d VO %d EDC %d networks.", providerId, voId, edcId);
        return Response.ok(json).build();
    }

    /**
     * Creates a new network for the provider's EDC.
     *
     * @return
     */
    @POST
    @Path("/networks")
    public Response createNetwork() throws Exception {
        URI resourceUri = new URI("/1");
        return Response.created(resourceUri).build();
    }

    /**
     * Sub-resource locator method. Returns the sub-resource object that can handle the remainder
     * of the request.
     *
     * @param networkId
     * @return
     */
    @Path("/networks/{nid}")
    public INetworkResource findNetwork(@PathParam("nid") int networkId) {
        return new NetworkResource(providerId, voId, edcId, networkId);
    }
}
