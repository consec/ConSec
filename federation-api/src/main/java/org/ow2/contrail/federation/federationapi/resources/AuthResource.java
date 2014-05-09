package org.ow2.contrail.federation.federationapi.resources;

import org.apache.log4j.Logger;
import org.consec.federationdb.model.Group;
import org.consec.federationdb.model.User;
import org.consec.federationdb.utils.EMF;
import org.json.JSONArray;
import org.ow2.contrail.federation.federationapi.authorization.Authorizer;
import org.ow2.contrail.federation.federationapi.utils.JSONObject;
import org.ow2.contrail.federation.federationapi.utils.RestUriBuilder;
import org.ow2.contrail.federation.herasafauthorizer.Rule;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.ContextLoader;

import javax.persistence.EntityManager;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.List;

@Path("/auth")
public class AuthResource {
    private static Logger log = Logger.getLogger(AuthResource.class);
    private Authorizer authorizer;

    public AuthResource() {
        ApplicationContext ctx = ContextLoader.getCurrentWebApplicationContext();
        authorizer = (Authorizer) ctx.getBean("authorizer");
    }

    /**
     * Returns list of all authorization rules for the specified user.
     *
     * @return
     */
    @GET
    @Path("/users/{userUuid}/rules")
    @Produces("application/json")
    public Response getUserRules(@PathParam("userUuid") String userUuid) throws Exception {

        EntityManager em = EMF.createEntityManager();
        try {
            User user = em.find(User.class, userUuid);
            if (user == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }

            List<Rule> rules = authorizer.getRules(user);
            JSONArray array = new JSONArray();
            for (Rule rule : rules) {
                JSONObject o = new JSONObject();
                o.put("id", rule.getRuleId());
                o.put("uri", RestUriBuilder.getAuthRuleUri(rule));
                array.put(o);
            }
            return Response.ok(array.toString()).build();
        }
        finally {
            EMF.closeEntityManager(em);
        }
    }

    /**
     * Adds a new authorization rule for the specified user.
     *
     * @return
     */
    @POST
    @Path("/users/{userUuid}/rules")
    @Consumes("application/json")
    public Response addUserRule(@PathParam("userUuid") String userUuid, String requestBody) throws Exception {

        EntityManager em = EMF.createEntityManager();
        try {
            User user = em.find(User.class, userUuid);
            if (user == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }

            Rule rule;
            try {
                rule = Rule.fromJSON(requestBody);
            }
            catch (Exception e) {
                throw new WebApplicationException(
                        Response.status(Response.Status.BAD_REQUEST).
                                entity("Invalid JSON data: " + e.getMessage()).build());
            }

            String ruleId = authorizer.deployRule(user, rule);

            URI resourceUri = new URI(ruleId);
            return Response.created(resourceUri).build();
        }
        finally {
            EMF.closeEntityManager(em);
        }
    }

    /**
     * Returns list of all authorization rules for the specified group.
     *
     * @return
     */
    @GET
    @Path("/groups/{groupId}/rules")
    @Produces("application/json")
    public Response getGroupRules(@PathParam("groupId") int groupId) throws Exception {

        EntityManager em = EMF.createEntityManager();
        try {
            Group group = em.find(Group.class, groupId);
            if (group == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }

            List<Rule> rules = authorizer.getRules(group);
            JSONArray array = new JSONArray();
            for (Rule rule : rules) {
                JSONObject o = new JSONObject();
                o.put("id", rule.getRuleId());
                o.put("uri", RestUriBuilder.getAuthRuleUri(rule));
                array.put(o);
            }
            return Response.ok(array.toString()).build();
        }
        finally {
            EMF.closeEntityManager(em);
        }
    }

    /**
     * Adds a new authorization rule for the specified group.
     *
     * @return
     */
    @POST
    @Path("/groups/{groupId}/rules")
    @Consumes("application/json")
    public Response addGroupRule(@PathParam("userUuid") int groupId, String requestBody) throws Exception {

        EntityManager em = EMF.createEntityManager();
        try {
            Group group = em.find(Group.class, groupId);
            if (group == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }

            Rule rule;
            try {
                rule = Rule.fromJSON(requestBody);
            }
            catch (Exception e) {
                throw new WebApplicationException(
                        Response.status(Response.Status.BAD_REQUEST).
                                entity("Invalid JSON data: " + e.getMessage()).build());
            }

            String ruleId = authorizer.deployRule(group, rule);

            URI resourceUri = new URI(ruleId);
            return Response.created(resourceUri).build();
        }
        finally {
            EMF.closeEntityManager(em);
        }
    }
}
