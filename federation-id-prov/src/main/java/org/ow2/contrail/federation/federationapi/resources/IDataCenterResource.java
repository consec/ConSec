package org.ow2.contrail.federation.federationapi.resources;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;

public interface IDataCenterResource {
    /**
     * Returns the JSON representation of the given data center.
     *
     * @return
     */
    @GET
    @Produces("application/json")
    Response getDC();

    /**
     * Updates the given data center.
     *
     * @return
     */
    @PUT
    Response updateDC();

    /**
     * Returns all storages of the data center.
     *
     * @return
     */
    @GET
    @Path("/storages")
    @Produces("application/json")
    Response getStorages();

    /**
     * Creates a new storage for the provider's data center.
     *
     * @return
     */
    @POST
    @Path("/storages")
    Response createStorage() throws Exception;

    /**
     * Sub-resource locator method. Returns the sub-resource object that can handle the remainder
     * of the request.
     *
     * @param storageId
     * @return
     */
    @Path("/storages/{sid}")
    IStorageResource findStorage(@PathParam("sid") int storageId);

    /**
     * Returns all networks of the data center.
     *
     * @return
     */
    @GET
    @Path("/networks")
    @Produces("application/json")
    Response getNetworks();

    /**
     * Creates a new network for the provider's data center.
     *
     * @return
     */
    @POST
    @Path("/networks")
    Response createNetwork() throws Exception;

    /**
     * Sub-resource locator method. Returns the sub-resource object that can handle the remainder
     * of the request.
     *
     * @param networkId
     * @return
     */
    @Path("/networks/{nid}")
    INetworkResource findNetwork(@PathParam("nid") int networkId);

    /**
     * Returns all clusters of the data center.
     *
     * @return
     */
    @GET
    @Path("/clusters")
    @Produces("application/json")
    Response getClusters();

    /**
     * Creates a new cluster for the provider's data center.
     *
     * @return
     */
    @POST
    @Path("/clusters")
    Response createCluster() throws Exception;

    /**
     * Sub-resource locator method. Returns the sub-resource object that can handle the remainder
     * of the request.
     *
     * @param clusterId
     * @return
     */
    @Path("/clusters/{cid}")
    IClusterResource findCluster(@PathParam("cid") int clusterId);
}
