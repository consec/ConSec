package org.ow2.contrail.federation.federationapi.resources;

import org.apache.log4j.Logger;
import org.ow2.contrail.federation.federationapi.authorization.Authorizer;
import org.ow2.contrail.federation.herasafauthorizer.Rule;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.ContextLoader;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;

@Path("/auth/rules/{ruleId}")
public class AuthRuleResource {
    private static Logger log = Logger.getLogger(AuthRuleResource.class);
    private String ruleId;
    private Authorizer authorizer;

    public AuthRuleResource(@PathParam("ruleId") String ruleId) {
        this.ruleId = ruleId;

        ApplicationContext ctx = ContextLoader.getCurrentWebApplicationContext();
        authorizer = (Authorizer) ctx.getBean("authorizer");
    }

    /**
     * Returns specified authorization rule details.
     *
     * @return
     */
    @GET
    @Produces("application/json")
    public Response getRule() throws Exception {
        if (log.isTraceEnabled()) {
            log.trace(String.format("getPolicyRule(ruleId=%s) started.", ruleId));
        }

        Rule rule = authorizer.getRule(ruleId);
        if (rule == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(rule.toJSON()).build();
    }

    /**
     * Updates specified authorization rule.
     *
     * @return
     */
    @PUT
    @Consumes("application/json")
    public Response updateRule(String requestBody) throws Exception {
        if (log.isTraceEnabled()) {
            log.trace(String.format("updateRule(ruleID=%s) started. Data: %s", ruleId, requestBody));
        }

        Rule rule = authorizer.getRule(ruleId);
        if (rule == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        // TODO: check if subject is the same

        Rule newRule;
        try {
            newRule = Rule.fromJSON(requestBody);
        }
        catch (Exception e) {
            throw new WebApplicationException(
                    Response.status(Response.Status.BAD_REQUEST).
                            entity("Invalid JSON data: " + e.getMessage()).build());
        }

        authorizer.updateRule(rule.getRuleId(), newRule);

        return Response.status(Response.Status.NO_CONTENT).build();
    }

    /**
     * Deletes specified authorization rule.
     *
     * @return
     */
    @DELETE
    public Response deleteRule() throws Exception {
        if (log.isTraceEnabled()) {
            log.trace(String.format("deleteRule(ruleId=%s) started.", ruleId));
        }

        Rule rule = authorizer.getRule(ruleId);
        if (rule == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        authorizer.removeRule(rule.getRuleId());

        return Response.status(Response.Status.NO_CONTENT).build();
    }
}
