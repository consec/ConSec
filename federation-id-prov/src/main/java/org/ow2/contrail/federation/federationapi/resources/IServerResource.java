package org.ow2.contrail.federation.federationapi.resources;

import org.json.JSONException;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;

public interface IServerResource {
    /**
     * Returns the JSON representation of the given server.
     *
     * @return
     */
    @GET
    @Produces("application/json")
    Response getServer() throws JSONException;

    /**
     * Updates the selected server.
     *
     * @return
     */
    @PUT
    @Consumes("application/json")
    Response updateServer(String requestBody) throws Exception;

    /**
     * Deletes selected server.
     *
     * @return
     */
    @DELETE
    Response removeServer() throws Exception;
}
