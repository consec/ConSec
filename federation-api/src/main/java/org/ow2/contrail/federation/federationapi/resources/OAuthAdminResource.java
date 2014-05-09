package org.ow2.contrail.federation.federationapi.resources;

import org.ow2.contrail.federation.federationapi.utils.Conf;
import org.ow2.contrail.federation.federationapi.utils.RestProxy;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

@Path("/oauthadmin")
public class OAuthAdminResource {

    @GET
    @Path("organizations")
    public Response getOrganizations(@Context HttpServletRequest httpServletRequest) throws Exception {
        return forwardToAS("/organizations", httpServletRequest);
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("organizations")
    public Response addOrganization(@Context HttpServletRequest httpServletRequest) throws Exception {
        return forwardToAS("/organizations", httpServletRequest);
    }

    @GET
    @Path("organizations/{orgId}")
    public Response getOrganization(@PathParam("orgId") int orgId, @Context HttpServletRequest httpServletRequest)
            throws Exception {
        return forwardToAS(String.format("/organizations/%d", orgId), httpServletRequest);
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("organizations/{orgId}")
    public Response updateOrganization(@PathParam("orgId") int orgId, @Context HttpServletRequest httpServletRequest)
            throws Exception {
        return forwardToAS(String.format("/organizations/%d", orgId), httpServletRequest);
    }

    @DELETE
    @Path("organizations/{orgId}")
    public Response deleteOrganization(@PathParam("orgId") int orgId, @Context HttpServletRequest httpServletRequest)
            throws Exception {
        return forwardToAS(String.format("/organizations/%d", orgId), httpServletRequest);
    }

    @GET
    @Path("organizations/{orgId}/clients")
    public Response getClients(@PathParam("orgId") int orgId, @Context HttpServletRequest httpServletRequest)
            throws Exception {
        return forwardToAS(String.format("/organizations/%d/clients", orgId), httpServletRequest);
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("organizations/{orgId}/clients")
    public Response addClient(@PathParam("orgId") int orgId, @Context HttpServletRequest httpServletRequest)
            throws Exception {
        return forwardToAS(String.format("/organizations/%d/clients", orgId), httpServletRequest);
    }

    @GET
    @Path("organizations/{orgId}/clients/{clientId}")
    public Response getClient(@PathParam("orgId") int orgId, @PathParam("clientId") int clientId,
                               @Context HttpServletRequest httpServletRequest) throws Exception {
        return forwardToAS(String.format("/organizations/%d/clients/%d", orgId, clientId), httpServletRequest);
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("organizations/{orgId}/clients/{clientId}")
    public Response updateClient(@PathParam("orgId") int orgId, @PathParam("clientId") int clientId,
                               @Context HttpServletRequest httpServletRequest) throws Exception {
        return forwardToAS(String.format("/organizations/%d/clients/%d", orgId, clientId), httpServletRequest);
    }

    @DELETE
    @Path("organizations/{orgId}/clients/{clientId}")
    public Response deleteClient(@PathParam("orgId") int orgId, @PathParam("clientId") int clientId,
                               @Context HttpServletRequest httpServletRequest) throws Exception {
        return forwardToAS(String.format("/organizations/%d/clients/%d", orgId, clientId), httpServletRequest);
    }

    @GET
    @Path("owners")
    public Response getOwners(@Context HttpServletRequest httpServletRequest) throws Exception {
        return forwardToAS("/owners", httpServletRequest);
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("owners")
    public Response addOwner(@Context HttpServletRequest httpServletRequest) throws Exception {
        return forwardToAS("/owners", httpServletRequest);
    }

    private Response forwardToAS(String path, HttpServletRequest httpServletRequest) throws Exception {
        URI baseUri = Conf.getInstance().getAddressOAuthAS().resolve("admin/");
        Map<String, String> rewriteRules = new HashMap<String, String>();
        rewriteRules.put("\"\\/organizations\\/", "\"/oauthadmin/organizations/");
        rewriteRules.put("\"\\/owners\\/", "\"/oauthadmin/owners/");

        Map<String, String> locationHeaderRewriteRules = new HashMap<String, String>();
        locationHeaderRewriteRules.put(baseUri + "organizations/", "/oauthadmin/organizations/");
        locationHeaderRewriteRules.put(baseUri + "owners/", "/oauthadmin/owners/");

        RestProxy restProxy = new RestProxy(baseUri, rewriteRules, locationHeaderRewriteRules);

        return restProxy.forward(path, httpServletRequest);
    }
}
