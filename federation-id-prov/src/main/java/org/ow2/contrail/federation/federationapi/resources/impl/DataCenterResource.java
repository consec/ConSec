package org.ow2.contrail.federation.federationapi.resources.impl;

import org.ow2.contrail.federation.federationapi.resources.IClusterResource;
import org.ow2.contrail.federation.federationapi.resources.IDataCenterResource;
import org.ow2.contrail.federation.federationapi.resources.INetworkResource;
import org.ow2.contrail.federation.federationapi.resources.IStorageResource;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.net.URI;

public class DataCenterResource implements IDataCenterResource {
    private int providerId;
    private int dcId;

    public DataCenterResource(int providerId, int dcId) {
        this.providerId = providerId;
        this.dcId = dcId;
    }

    /**
     * Returns the JSON representation of the given data center.
     *
     * @return
     */
    @GET
    @Produces("application/json")
    public Response getDC() {
        String json = String.format("Provider %d DC %d",
                providerId, dcId);
        return Response.ok(json).build();
    }

    /**
     * Updates the given data center.
     *
     * @return
     */
    @PUT
    public Response updateDC() {
        return Response.ok().build();
    }

    /**
     * Returns all storages of the data center.
     *
     * @return
     */
    @GET
    @Path("/storages")
    @Produces("application/json")
    public Response getStorages() {
        String json = String.format("Provider %d DC %d storages.", providerId, dcId);
        return Response.ok(json).build();
    }

    /**
     * Creates a new storage for the provider's data center.
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
        return new StorageResource(providerId, dcId, storageId);
    }

    /**
     * Returns all networks of the data center.
     *
     * @return
     */
    @GET
    @Path("/networks")
    @Produces("application/json")
    public Response getNetworks() {
        String json = String.format("Provider %d DC %d networks.", providerId, dcId);
        return Response.ok(json).build();
    }

    /**
     * Creates a new network for the provider's data center.
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
        return new NetworkResource(providerId, dcId, networkId);
    }

    /**
     * Returns all clusters of the data center.
     *
     * @return
     */
    @GET
    @Path("/clusters")
    @Produces("application/json")
    public Response getClusters() {
        String json = String.format("Provider %d DC %d clusters.", providerId, dcId);
        return Response.ok(json).build();
    }

    /**
     * Creates a new cluster for the provider's data center.
     *
     * @return
     */
    @POST
    @Path("/clusters")
    public Response createCluster() throws Exception {
        URI resourceUri = new URI("/1");
        return Response.created(resourceUri).build();
    }

    /**
     * Sub-resource locator method. Returns the sub-resource object that can handle the remainder
     * of the request.
     *
     * @param clusterId
     * @return
     */
    @Path("/clusters/{cid}")
    public IClusterResource findCluster(@PathParam("cid") int clusterId) {
        return new ClusterResource(providerId, dcId, clusterId);
    }
}
