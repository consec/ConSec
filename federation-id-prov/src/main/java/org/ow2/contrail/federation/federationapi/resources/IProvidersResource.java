package org.ow2.contrail.federation.federationapi.resources;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;

public interface IProvidersResource {
    /**
     * Returns list of all providers the current user has access to.
     *
     * @return
     */
    @GET
    @Produces("application/json")
    Response getProviders() throws Exception;

    /**
     * Creates a new provider.
     *
     * @return
     */
    @POST
    @Consumes("application/json")
    public Response addProvider(String requestBody) throws Exception;

    /**
     * Sub-resource locator method. Returns the sub-resource object that can handle the remainder
     * of the request.
     *
     * @param providerId
     * @return
     */
    @Path("/{providerId}")
    IProviderResource findProvider(@PathParam("providerId") int providerId);
}
