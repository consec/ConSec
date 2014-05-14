package org.ow2.contrail.federation.federationapi.resources;

import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

public interface IStorageResource {
    /**
     * Returns the JSON representation of the given storage.
     *
     * @return
     */
    @GET
    @Produces("application/json")
    Response getStorage();

    /**
     * Updates the given storage.
     *
     * @return
     */
    @PUT
    Response updateStorage();
}
