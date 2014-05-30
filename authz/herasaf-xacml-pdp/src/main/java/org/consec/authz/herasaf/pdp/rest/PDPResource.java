package org.consec.authz.herasaf.pdp.rest;

import org.codehaus.jettison.json.JSONObject;
import org.consec.authz.herasaf.pdp.core.AuthSubject;
import org.consec.authz.herasaf.pdp.core.HerasafXACMLEngine;
import org.herasaf.xacml.core.context.impl.DecisionType;

import javax.servlet.ServletContext;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

@Path("/pdp")
public class PDPResource {

    @Context
    ServletContext context;

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("evaluate")
    public JSONObject evaluateAccessRequest(JSONObject json) throws Exception {
        AuthSubject subject = AuthSubject.fromJson(json.getJSONArray("subject"));
        String resource = json.getString("resource");
        String action = json.getString("action");

        HerasafXACMLEngine herasafXACMLEngine = (HerasafXACMLEngine) context.getAttribute("herasafXACMLEngine");
        DecisionType decision = herasafXACMLEngine.evaluateAccessRequest(subject, resource, action);

        JSONObject result = new JSONObject();
        result.put("decision", decision.value());
        return result;
    }
}
