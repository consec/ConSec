package org.ow2.contrail.federation.federationapi.resources;

import org.json.JSONException;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;

public interface IVirtualOrganizationResource {
    /**
     * Returns the JSON representation of the provider's virtual organization.
     *
     * @return
     */
    @GET
    @Produces("application/json")
    Response getVO() throws JSONException;

    /**
     * Updates the selected VO.
     *
     * @return
     */
    @PUT
    @Consumes("application/json")
    Response updateVO(String requestBody) throws Exception;

    /**
     * Deletes selected VO.
     *
     * @return
     */
    @DELETE
    Response removeVO() throws Exception;

    /**
     * Returns all clusters registered at the given VO.
     *
     * @return
     */
    @GET
    @Path("/clusters")
    @Produces("application/json")
    Response getClusters() throws Exception;

    /**
     * Registers cluster at the selected VO.
     *
     * @return
     */
    @POST
    @Path("/clusters")
    @Consumes("application/json")
    Response registerCluster(String requestBody) throws Exception;

    /**
     * Returns data about specific cluster registration
     *
     * @return
     */
    @GET
    @Path("/clusters/{id}")
    @Produces("application/json")
    Response getClusterRegistration(@PathParam("id") int id) throws JSONException;

    /**
     * Unregisters given cluster from given VO.
     *
     * @return
     */
    @DELETE
    @Path("/clusters/{id}")
    @Produces("application/json")
    Response unregisterCluster(@PathParam("id") int id) throws JSONException;

    /**
     * Returns a list of all CEEs for the selected provider and virtual organization.
     *
     * @return
     */
    @GET
    @Path("/cees")
    @Produces("application/json")
    Response getCEEs
    ();

    /**
     * Creates a new CEE for the selected provider and virtual organization.
     *
     * @return
     */
    @POST
    @Path("/cees")
    Response createCEE() throws Exception;

    /**
     * Sub-resource locator method. Returns the sub-resource object that can handle the remainder
     * of the request.
     *
     * @param ceeId
     * @return
     */
    @Path("/cees/{ceeId}")
    ICEEResource findCEE(@PathParam("ceeId") int ceeId);

    /**
     * Returns a list of elastic DCs for the provider's VO.
     *
     * @return
     */
    @GET
    @Path("/edcs")
    @Produces("application/json")
    Response getEDCs();

    /**
     * Creates a new EDC for the selected provider.
     *
     * @return
     */
    @POST
    @Path("/edcs")
    Response createEDC() throws Exception;

    /**
     * Sub-resource locator method. Returns the sub-resource object that can handle the remainder
     * of the request.
     *
     * @param edcId
     * @return
     */
    @Path("/edcs/{edcId}")
    IEDCResource findEDC(
            @PathParam("edcId") int edcId);

    /**
     * Returns all attributes of the provider's VO.
     *
     * @return
     */
    @GET
    @Path("/attributes")
    @Produces("application/json")
    Response getAttributes();

    /**
     * Updates attributes of the provider's VO.
     *
     * @return
     */
    @PUT
    @Path("/attributes")
    Response updateAttributes() throws Exception;
}
