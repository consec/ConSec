package org.ow2.contrail.federation.federationapi.resources;

import org.json.JSONException;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;

public interface IClusterResource {
    /**
     * Returns the JSON representation of the given cluster.
     *
     * @return
     */
    @GET
    @Produces("application/json")
    Response getCluster() throws JSONException;

    /**
     * Updates the given cluster.
     *
     * @return
     */
    @PUT
    @Consumes("application/json")
    Response updateCluster(String requestBody) throws Exception;

    /**
     * Deletes selected cluster.
     *
     * @return
     */
    @DELETE
    Response removeCluster() throws Exception;

    /**
     * Returns all servers registered at the given cluster.
     *
     * @return
     */
    @GET
    @Path("/servers")
    @Produces("application/json")
    Response getServers() throws Exception;

    /**
     * Registers server at the selected cluster.
     *
     * @return
     */
    @POST
    @Path("/servers")
    @Consumes("application/json")
    Response registerServer(String requestBody) throws Exception;

    /**
     * Returns data about specific server registration
     *
     * @return
     */
    @GET
    @Path("/servers/{id}")
    @Produces("application/json")
    Response getServerRegistration(@PathParam("id") int id) throws JSONException;

    /**
     * Unregisters given server from given cluster.
     *
     * @return
     */
    @DELETE
    @Path("/servers/{id}")
    @Produces("application/json")
    Response unregisterServer(@PathParam("id") int id) throws JSONException;

    /**
     * Returns all VMs registered at the given cluster.
     *
     * @return
     */
    @GET
    @Path("/vms")
    @Produces("application/json")
    Response getVMs() throws Exception;

    /**
     * Registers VM at the selected cluster.
     *
     * @return
     */
    @POST
    @Path("/vms")
    @Consumes("application/json")
    Response registerVM(String requestBody) throws Exception;

    /**
     * Returns data about specific VM registration
     *
     * @return
     */
    @GET
    @Path("/vms/{id}")
    @Produces("application/json")
    Response getVMRegistration(@PathParam("id") int id) throws JSONException;

    /**
     * Unregisters given VM from given cluster.
     *
     * @return
     */
    @DELETE
    @Path("/vms/{id}")
    @Produces("application/json")
    Response unregisterVM(@PathParam("id") int id) throws JSONException;

    /**
     * Returns all storages of the selected cluster.
     *
     * @return
     */
    @GET
    @Path("/storages")
    @Produces("application/json")
    Response getStorages();

    /**
     * Creates a new storage for the selected cluster.
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
     * Returns all networks of the selected cluster.
     *
     * @return
     */
    @GET
    @Path("/networks")
    @Produces("application/json")
    Response getNetworks();

    /**
     * Creates a new network for the selected cluster.
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
