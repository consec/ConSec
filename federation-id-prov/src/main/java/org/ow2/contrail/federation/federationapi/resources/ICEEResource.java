package org.ow2.contrail.federation.federationapi.resources;

import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

public interface ICEEResource {
    /**
     * Returns the JSON representation of the given CEE.
     *
     * @return
     */
    @GET
    @Produces("application/json")
    Response getCEE();

    /**
     * Updates the given CEE.
     *
     * @return
     */
    @PUT
    Response updateCEE();
}
