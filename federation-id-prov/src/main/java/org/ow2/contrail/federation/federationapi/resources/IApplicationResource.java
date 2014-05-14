package org.ow2.contrail.federation.federationapi.resources;

import org.ow2.contrail.federation.federationapi.interfaces.BaseSingle;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;

/**
 * @author ales
 */
public interface IApplicationResource extends BaseSingle {

    @GET
    @Produces("application/json")
    @Path("/ovfs")
    public Response getOvfs() throws Exception;

    /**
     * Gets the OVF.
     *
     * @return
     */
    @GET
    @Produces("application/json")
    @Path("/ovfs/{ovfId}")
    public Response getOvf(@PathParam("ovfId") int ovfId) throws Exception;

    /**
     * Gets the OVF.
     *
     * @return
     */
    @DELETE
    @Produces("application/json")
    @Path("/ovfs/{ovfId}")
    public Response deleteOvf(@PathParam("ovfId") int ovfId) throws Exception;


}
