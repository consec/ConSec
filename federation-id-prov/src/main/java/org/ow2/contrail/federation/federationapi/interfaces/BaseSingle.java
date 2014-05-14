/**
 *
 */
package org.ow2.contrail.federation.federationapi.interfaces;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.util.ArrayList;

/**
 * @author ales
 */
public interface BaseSingle {

    /**
     * Returns list of possible subresources.
     *
     * @return
     */
    public ArrayList<String> getSubresources() throws Exception;

    @GET
    @Produces("application/json")
    public Response get() throws Exception;

    @DELETE
    public Response delete() throws Exception;

    @PUT
    @Consumes("application/json")
    @Produces("application/json")
    public Response put(String content) throws Exception;

}
