package org.ow2.contrail.federation.federationapi.resources;

import org.json.JSONException;
import org.ow2.contrail.federation.federationapi.resources.impl.OvfResource;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;

public interface IProviderResource {
    /**
     * Returns the JSON representation of the provider.
     *
     * @return
     */
    @GET
    @Produces("application/json")
    Response getProvider() throws JSONException;

    /**
     * Updates the selected provider.
     *
     * @return
     */
    @PUT
    @Consumes("application/json")
    Response updateProvider(String requestBody) throws Exception;

    /**
     * Deletes selected provider.
     *
     * @return
     */
    @DELETE
    Response removeProvider() throws Exception;

    /**
     * Return a list of all virtual organizations for the provider.
     *
     * @return
     */
    @GET
    @Path("/vos")
    @Produces("application/json")
    Response getVOs() throws Exception;

    /**
     * Creates a new virtual organization for the provider.
     *
     * @return
     */
    @POST
    @Path("/vos")
    @Consumes("application/json")
    Response addVO(String requestBody) throws Exception;

    /**
     * Sub-resource locator method. Returns the sub-resource object that can handle the remainder
     * of the request.
     *
     * @param voId
     * @return
     */
    @Path("/vos/{voId}")
    IVirtualOrganizationResource findVO(@PathParam("voId") int voId);

    /**
     * Return a list of all users for the selected provider.
     *
     * @return
     */
    @GET
    @Path("/users")
    @Produces("application/json")
    Response getUsers();

    /**
     * Links new user with the provider or updates existing one.
     *
     * @return
     */
    @POST
    @Path("/users")
    Response addUser() throws Exception;

    /**
     * Sub-resource locator method. Returns the sub-resource object that can handle the remainder
     * of the request.
     *
     * @param userId
     * @return
     */
    @Path("/users/{uid}")
    IUserResource findUser(@PathParam("uid") int userId);

    /**
     * Returns a list of all data centers for the selected provider.
     *
     * @return
     */
    @GET
    @Path("/dcs")
    @Produces("application/json")
    Response getDataCenters();

    /**
     * Links new data center with the provider or updates existing one.
     *
     * @return
     */
    @POST
    @Path("/dcs")
    Response addDataCenter() throws Exception;

    /**
     * Sub-resource locator method. Returns the sub-resource object that can handle the remainder
     * of the request.
     *
     * @param dcId
     * @return
     */
    @Path("/dcs/{dcid}")
    IDataCenterResource findDataCenter(@PathParam("dcid") int dcId);

    /**
     * Returns all provider's virtual machines.
     *
     * @return
     */
    @GET
    @Path("/vms")
    @Produces("application/json")
    Response getVMs() throws Exception;

    /**
     * Creates a new virtual machine for the selected provider.
     *
     * @return
     */
    @POST
    @Path("/vms")
    @Consumes("application/json")
    Response addVM(String requestBody) throws Exception;

    /**
     * Sub-resource locator method. Returns the sub-resource object that can handle the remainder
     * of the request.
     *
     * @param vmId
     * @return
     */
    @Path("/vms/{vmid}")
    IVmResource findVm(@PathParam("vmid") int vmId);

    /**
     * Returns all storages of the selected provider.
     *
     * @return
     */
    @GET
    @Path("/storages")
    @Produces("application/json")
    Response getStorages();

    /**
     * Creates a new storage for the selected provider.
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
     * Returns all networks of the selected provider.
     *
     * @return
     */
    @GET
    @Path("/networks")
    @Produces("application/json")
    Response getNetworks();

    /**
     * Creates a new network for the selected provider.
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
     * Returns all provider's servers.
     *
     * @return
     */
    @GET
    @Path("/servers")
    @Produces("application/json")
    Response getServers() throws Exception;

    /**
     * Creates a new server for the given provider.
     *
     * @return
     */
    @POST
    @Path("/servers")
    @Consumes("application/json")
    Response addServer(String requestBody) throws Exception;

    /**
     * Sub-resource locator method. Returns the sub-resource object that can handle the remainder
     * of the request.
     *
     * @param serverId
     * @return
     */
    @Path("/servers/{sid}")
    IServerResource findServer(@PathParam("sid") int serverId);

    /**
     * Returns all provider's clusters.
     *
     * @return
     */
    @GET
    @Path("/clusters")
    @Produces("application/json")
    Response getClusters() throws Exception;

    /**
     * Creates a new cluster for the selected provider.
     *
     * @return
     */
    @POST
    @Path("/clusters")
    @Consumes("application/json")
    Response addCluster(String requestBody) throws Exception;

    /**
     * Sub-resource locator method. Returns the sub-resource object that can handle the remainder
     * of the request.
     *
     * @param clusterId
     * @return
     */
    @Path("/clusters/{cid}")
    IClusterResource findCluster(@PathParam("cid") int clusterId);

    /**
     * Returns all provider's OVFs.
     * Due to the move of OVFs under Application, this method is not working
     * properly.
     *
     * @return
     * @deprecated
     */
    @GET
    @Path("/ovfs")
    @Produces("application/json")
    @Deprecated
    Response getOvfs() throws Exception;

    /**
     * Creates a new OVF for the given provider.
     * Due to the move of OVFs under Application, this method is not working
     * properly.
     *
     * @return
     * @deprecated
     */
    @POST
    @Path("/ovfs")
    @Consumes("application/json")
    @Deprecated
    Response addOvf(String requestBody) throws Exception;

    /**
     * Sub-resource locator method. Returns the sub-resource object that can handle the remainder
     * of the request.
     *
     * @param ovfId
     * @return
     * @deprecated
     */
    @Deprecated
    @Path("/ovfs/{oid}")
    OvfResource findOvf(@PathParam("oid") int ovfId);

    /**
     * Returns a list of all registered SLA templates for the selected provider.
     *
     * @return
     */
    @GET
    @Path("/slats")
    @Produces("application/json")
    Response getSLATemplates() throws Exception;

    /**
     * Registers a new SLA template at the selected provider.
     *
     * @return
     */
    @POST
    @Path("/slats")
    @Consumes("application/json")
    Response addSLATemplate(String requestBody) throws Exception;

    /**
     * Sub-resource locator method. Returns the sub-resource object that can handle the remainder
     * of the request.
     *
     * @param slatId
     * @return
     */
    @Path("/slats/{slatId}")
    ISLATemplateResource findSLATemplate(@PathParam("slatId") int slatId);
}
