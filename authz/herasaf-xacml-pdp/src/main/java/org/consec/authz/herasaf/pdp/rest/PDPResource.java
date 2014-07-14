package org.consec.authz.herasaf.pdp.rest;

import org.codehaus.jettison.json.JSONObject;
import org.consec.authz.herasaf.pdp.core.AuthSubject;
import org.consec.authz.herasaf.pdp.core.HerasafXACMLEngine;
import org.herasaf.xacml.core.context.impl.DecisionType;

import javax.servlet.ServletContext;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/pdp")
public class PDPResource {

    @Context
    ServletContext context;

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("evaluate")
    public JSONObject evaluateAccessRequest(JSONObject json) throws Exception {
        AuthSubject subject = null;
        String resource = null;
        String action = null;
        try {
            subject = AuthSubject.fromJson(json.getJSONArray("subject"));
            resource = json.getString("resource");
            action = json.getString("action");
        }
        catch (Exception e) {
            throw new WebApplicationException(Response.status(
                    Response.Status.BAD_REQUEST).entity(e.getMessage()).build());
        }

        HerasafXACMLEngine herasafXACMLEngine = (HerasafXACMLEngine) context.getAttribute("herasafXACMLEngine");
        DecisionType decision = herasafXACMLEngine.evaluateAccessRequest(subject, resource, action);

        JSONObject result = new JSONObject();
        result.put("decision", decision.name());
        return result;
    }
}
