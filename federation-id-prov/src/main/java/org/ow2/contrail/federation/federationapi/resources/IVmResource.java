package org.ow2.contrail.federation.federationapi.resources;

import org.json.JSONException;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;

public interface IVmResource {
    /**
     * Returns the JSON representation of the selected VM.
     *
     * @return
     */
    @GET
    @Produces("application/json")
    Response getVm() throws JSONException;

    /**
     * Updates the selected VM.
     *
     * @return
     */
    @PUT
    @Consumes("application/json")
    Response updateVm(String requestBody) throws Exception;

    /**
     * Deletes selected VM.
     *
     * @return
     */
    @DELETE
    Response removeVm() throws Exception;

    /**
     * Returns the VMEx.
     *
     * @return
     */
    @GET
    @Path("/vmex")
    @Produces("application/json")
    Response getVmex();

    /**
     * Updates the VMEx.
     *
     * @return
     */
    @PUT
    @Path("/vmex")
    Response updateVmex() throws Exception;

    /**
     * Returns the vmslot.
     *
     * @return
     */
    @GET
    @Path("/vmslot")
    @Produces("application/json")
    Response getVmslot();

    /**
     * Updates the vmslot.
     *
     * @return
     */
    @PUT
    @Path("/vmslot")
    Response updateVmslot() throws Exception;

    /**
     * Returns the vmapp.
     *
     * @return
     */
    @GET
    @Path("/vmapp")
    @Produces("application/json")
    Response getVmapp();

    /**
     * Updates the vmapp.
     *
     * @return
     */
    @PUT
    @Path("/vmapp")
    Response updateVmapp() throws Exception;
}
