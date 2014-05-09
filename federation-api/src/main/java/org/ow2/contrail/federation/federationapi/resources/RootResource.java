package org.ow2.contrail.federation.federationapi.resources;

import org.ow2.contrail.federation.federationapi.utils.JSONObject;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

@Path("/")
public class RootResource {

    @GET
    @Produces("application/json")
    public Response get() throws Exception {
        JSONObject json = new JSONObject();
        json.put("users", "/users");
        json.put("attributes", "/attributes");
        json.put("idps", "/idps");
        json.put("roles", "/roles");
        json.put("groups", "/groups");

        return Response.ok(json).build();
    }
}
