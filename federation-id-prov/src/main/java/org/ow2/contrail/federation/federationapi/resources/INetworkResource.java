package org.ow2.contrail.federation.federationapi.resources;

import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

public interface INetworkResource {
    /**
     * Returns the JSON representation of the given network.
     *
     * @return
     */
    @GET
    @Produces("application/json")
    Response getNetwork();

    /**
     * Updates the given network.
     *
     * @return
     */
    @PUT
    Response updateNetwork();
}
