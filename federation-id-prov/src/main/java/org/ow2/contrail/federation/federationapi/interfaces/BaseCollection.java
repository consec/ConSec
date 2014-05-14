/**
 *
 */
package org.ow2.contrail.federation.federationapi.interfaces;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;

/**
 * @author ales
 */
public interface BaseCollection {


    /**
     * Returns list of collection.
     *
     * @return
     */
    @GET
    @Produces("application/json")
    public Response get() throws Exception;

    /**
     * Creates a new entry in a collection.
     *
     * @return
     */
    @POST
    @Consumes("application/json")
    @Produces("application/json")
    public Response post(String content) throws Exception;

    /**
     * Sub-resource locator method. Returns the sub-resource object that can handle the remainder
     * of the request.
     *
     * @param userId
     * @return
     */
    @Path("/{id}")
    public BaseSingle subResource(@PathParam("id") int id) throws Exception;

}
