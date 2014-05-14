package org.ow2.contrail.federation.federationapi.resources;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;

public interface ISLATemplateResource {
    /**
     * Returns the JSON representation of the given SLA template.
     *
     * @return
     */
    @GET
    @Produces("application/json")
    Response getSLATemplate() throws Exception;

    /**
     * Updates the selected SLA template.
     *
     * @return
     */
    @PUT
    @Consumes("application/json")
    Response updateSLATemplate(String requestBody) throws Exception;

    /**
     * Unregisters selected SLA template.
     *
     * @return
     */
    @DELETE
    Response removeSLATemplate() throws Exception;
}
