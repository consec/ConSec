package org.ow2.contrail.federation.federationapi.resources;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;

public interface IEDCResource {
    /**
     * Returns the JSON representation of the elastic DC for the provider's VO.
     *
     * @return
     */
    @GET
    @Produces("application/json")
    Response getEDC();

    /**
     * Updates the provider’s elastic DC with the given ID.
     *
     * @return
     */
    @PUT
    Response updateEDC();

    /**
     * Returns all storages of the EDC.
     *
     * @return
     */
    @GET
    @Path("/storages")
    @Produces("application/json")
    Response getStorages();

    /**
     * Creates a new storage for the provider's EDC.
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
     * Returns all networks of the EDC.
     *
     * @return
     */
    @GET
    @Path("/networks")
    @Produces("application/json")
    Response getNetworks();

    /**
     * Creates a new network for the provider's EDC.
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
}
