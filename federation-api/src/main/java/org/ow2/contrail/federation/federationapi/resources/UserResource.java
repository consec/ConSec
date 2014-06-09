package org.ow2.contrail.federation.federationapi.resources;

import org.apache.log4j.Logger;
import org.consec.federationdb.model.*;
import org.consec.federationdb.utils.EMF;
import org.json.JSONArray;
import org.mindrot.jbcrypt.BCrypt;
import org.ow2.contrail.federation.federationapi.utils.*;

import javax.persistence.EntityManager;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

@Path("/users/{userUuid}")
public class UserResource {

    protected static Logger logger = Logger.getLogger(UserResource.class);

    private String userUuid;

    public UserResource(@PathParam("userUuid") String userUuid) {
        this.userUuid = userUuid;
    }

    @GET
    @Produces("application/json")
    public Response get() throws Exception {

        EntityManager em = EMF.createEntityManager();

        try {
            User user = em.find(User.class, userUuid);
            if (user == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }

            String baseUri = RestUriBuilder.getUserUri(user);
            JSONObject json = new JSONObject();
            json.put("userId", user.getUserId());
            json.put("username", user.getUsername());
            json.put("firstName", user.getFirstName());
            json.put("lastName", user.getLastName());
            json.put("email", user.getEmail());
            json.put("password", user.getPassword());
            json.put("attributes", baseUri + "/attributes");
            json.put("ids", baseUri + "/ids");
            // roles
            JSONArray rolesArray = new JSONArray();
            for (Role role : user.getRoleList()) {
                rolesArray.put(role.getName());
            }
            json.put("roles", rolesArray);
            // groups
            JSONArray groupsArray = new JSONArray();
            for (Group group : user.getGroupList()) {
                groupsArray.put(group.getName());
            }
            json.put("groups", groupsArray);

            return Response.ok(json.toString()).build();
        }
        finally {
            EMF.closeEntityManager(em);
        }
    }

    @DELETE
    public Response delete() throws Exception {

        EntityManager em = EMF.createEntityManager();

        try {
            User user = em.find(User.class, userUuid);
            if (user == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }

            em.getTransaction().begin();
            em.remove(user);
            em.getTransaction().commit();

            return Response.status(Response.Status.NO_CONTENT).build();
        }
        finally {
            EMF.closeEntityManager(em);
        }
    }

    @PUT
    @Consumes("application/json")
    @Produces("application/json")
    public Response put(JSONObject userData) throws Exception {

        EntityManager em = EMF.createEntityManager();

        try {
            User user = em.find(User.class, userUuid);
            if (user == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }

            em.getTransaction().begin();

            if (userData.has("username"))
                user.setUsername((String) userData.get("username"));
            if (userData.has("firstName"))
                user.setFirstName((String) userData.get("firstName"));
            if (userData.has("attributes"))
                user.setAttributes((String) userData.get("attributes"));
            if (userData.has("lastName"))
                user.setLastName((String) userData.get("lastName"));
            if (userData.has("password")) {
                String plain_text_password = (String) userData.get("password");
                // Hash a password for the first time
                // gensalt's log_rounds parameter determines the complexity
                // the work factor is 2**log_rounds, and the default is 10
                String hashed = BCrypt.hashpw(plain_text_password, BCrypt.gensalt(12));
                user.setPassword(hashed);
            }
            if (userData.has("email"))
                user.setEmail((String) userData.get("email"));

            em.getTransaction().commit();

            return Response.status(Response.Status.NO_CONTENT).build();
        }
        finally {
            EMF.closeEntityManager(em);
        }
    }

    @GET
    @Produces("application/json")
    @Path("/attributes")
    public Response getAttributes() throws Exception {

        EntityManager em = EMF.createEntityManager();

        try {
            User user = em.find(User.class, userUuid);
            if (user == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }

            JSONArray json = new JSONArray();
            for (UserHasAttribute userAttribute : user.getUserHasAttributeList()) {
                String uri = String.format("%s/attributes/%s", RestUriBuilder.getUserUri(user),
                        userAttribute.getUserHasAttributePK().getAttributeId());

                JSONObject o = new JSONObject();
                o.put("name", userAttribute.getAttribute().getName());
                o.put("uri", uri);
                json.put(o);
            }
            return Response.ok(json.toString()).build();
        }
        finally {
            EMF.closeEntityManager(em);
        }
    }

    @POST
    @Consumes("application/json")
    @Produces("application/json")
    @Path("/attributes")
    public Response postAttribute(JSONObject attrData) throws Exception {

        EntityManager em = EMF.createEntityManager();
        try {
            User user = em.find(User.class, userUuid);
            if (user == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }

            UserHasAttribute attribute = new UserHasAttribute();

            if (attrData.has("attributeUuid")) {
                attribute.setUserHasAttributePK(
                        new UserHasAttributePK(user.getUserId(), attrData.getString("attributeUuid")));
            }
            else {
                logger.error("The attribute 'attributeUuid' is missing.");
                return Response.status(Response.Status.BAD_REQUEST).build();
            }

            if (attrData.has("value"))
                attribute.setValue((String) attrData.get("value"));

            attribute.setUser(user);

            if (attrData.has("referenceId")) {
                logger.debug("Setting referenceId");
                int referenceId = FederationDBCommon.getIdFromString(attrData.getString("referenceId"));
                attribute.setReferenceId(referenceId);
            }

            em.getTransaction().begin();
            em.persist(attribute);
            user.getUserHasAttributeList().add(attribute);
            em.getTransaction().commit();

            URI resourceUri = new URI(String.format("/%s", attribute.getUserHasAttributePK().getAttributeId()));
            return Response.created(resourceUri).build();
        }
        finally {
            EMF.closeEntityManager(em);
        }
    }

    @GET
    @Produces("application/json")
    @Path("/ids")
    public Response getIds() throws Exception {

        EntityManager em = EMF.createEntityManager();
        try {
            User user = em.find(User.class, userUuid);
            if (user == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }

            JSONArray json = new JSONArray();
            String userUri = RestUriBuilder.getUserUri(user);
            for (UserHasIdentityProvider idp : user.getUserHasIdentityProviderList()) {
                String uri = String.format("%s/ids/%s", userUri,
                        idp.getUserHasIdentityProviderPK().getIdpId());
                JSONObject o = new JSONObject();
                o.put("identity", idp.getIdentity());
                o.put("uri", uri);
                json.put(o);
            }
            return Response.ok(json.toString()).build();
        }
        finally {
            EMF.closeEntityManager(em);
        }
    }

    @POST
    @Consumes("application/json")
    @Produces("application/json")
    @Path("/ids")
    public Response postId(JSONObject idData) throws Exception {

        EntityManager em = EMF.createEntityManager();
        try {
            User user = em.find(User.class, userUuid);
            if (user == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }

            UserHasIdentityProvider userIdp = new UserHasIdentityProvider();
            if (!idData.has("identityProviderId")) {
                logger.error("identityProviderId not provided");
                return Response.status(Response.Status.BAD_REQUEST).build();
            }
            // Get Idp
            String idpId = idData.getString("identityProviderId");
            IdentityProvider idp = em.find(IdentityProvider.class, idpId);
            if (idp == null) {
                logger.error("Identity Provider with id " + idData.getString("identityProviderId") + " not found.");
                return Response.status(Response.Status.NOT_FOUND).build();
            }

            UserHasIdentityProvider uhasidp = em.find(UserHasIdentityProvider.class, 
                    new UserHasIdentityProviderPK(userUuid, idp.getIdpId())); 
            if (uhasidp != null) {
                logger.debug("User already has this provider registered");
                // resource is already registered
                return Response.status(Response.Status.CONFLICT).build();
            }
            
            userIdp.setIdentityProvider(idp);
            userIdp.setUserHasIdentityProviderPK(new UserHasIdentityProviderPK(userUuid, idpId));
            if (idData.has("identity")) {
                userIdp.setIdentity(idData.getString("identity"));
            }
            else {
                //user identity is mandatory
                logger.error("missing identity - it is mandatory.");
                return Response.status(Response.Status.BAD_REQUEST).build();
            }
            if (idData.has("attributes"))
                userIdp.setAttributes(idData.getString("attributes"));

            em.getTransaction().begin();
            em.persist(userIdp);
            user.getUserHasIdentityProviderList().add(userIdp);
            idp.getUserHasIdentityProviderList().add(userIdp);
            em.getTransaction().commit();

            URI resourceUri = new URI(String.format("/%s", userIdp.getIdentityProvider().getIdpId()));
            logger.debug("Exiting post");
            return Response.created(resourceUri).build();
        }
        finally {
            EMF.closeEntityManager(em);
        }
    }

    @GET
    @Produces("application/json")
    @Path("/roles")
    public Response getRoles() throws Exception {
        EntityManager em = EMF.createEntityManager();
        try {
            User user = em.find(User.class, userUuid);
            if (user == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }

            JSONArray UriList = new JSONArray();
            for (Role role : user.getRoleList()) {
                String uri = String.format("/roles/%d", role.getRoleId());
                JSONObject o = new JSONObject();
                o.put("name", role.getName());
                o.put("uri", uri);
                UriList.put(o);
            }
            return Response.ok(UriList.toString()).build();
        }
        finally {
            EMF.closeEntityManager(em);
        }
    }

    @POST
    @Consumes("application/json")
    @Produces("application/json")
    @Path("/roles")
    public Response postRole(JSONObject roleData) throws Exception {

        if (!roleData.has("roleId")) {
            logger.error("User role ID has to be provided.");
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        EntityManager em = EMF.createEntityManager();
        try {
            User user = em.find(User.class, userUuid);
            if (user == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }

            String roleString = roleData.getString("roleId");
            int roleId = Integer.parseInt(roleString.substring(roleString.lastIndexOf("/") + 1));
            logger.debug("Got Role Id: " + roleId);
            Role newrole = em.find(Role.class, roleId);

            em.getTransaction().begin();
            user.getRoleList().add(newrole);
            newrole.getUserList().add(user);
            em.getTransaction().commit();

            URI resourceUri = new URI(String.format("/%d", newrole.getRoleId()));
            return Response.created(resourceUri).build();
        }
        finally {
            EMF.closeEntityManager(em);
        }
    }

    @GET
    @Produces("application/json")
    @Path("/roles/{roleId}")
    public Response getRole(@PathParam("roleId") int roleId) throws Exception {
        return new UserRoleResource(roleId).get();
    }

    @DELETE
    @Path("/roles/{roleId}")
    public Response deleteUserRole(@PathParam("roleId") int roleId) throws Exception {
        EntityManager em = EMF.createEntityManager();
        try {
            User user = em.find(User.class, userUuid);
            if (user == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            Role role = em.find(Role.class, roleId);
            if (role == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }

            logger.debug(String.format("Deleting role %d for user %s", role.getRoleId(), user.getUserId()));

            em.getTransaction().begin();
            user.getRoleList().remove(role);
            role.getUserList().remove(user);
            em.getTransaction().commit();

            return Response.status(Response.Status.NO_CONTENT).build();
        }
        finally {
            EMF.closeEntityManager(em);
        }
    }

    @GET
    @Produces("application/json")
    @Path("/groups")
    public Response getGroups() throws Exception {
        EntityManager em = EMF.createEntityManager();
        try {
            User user = em.find(User.class, userUuid);
            if (user == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }

            JSONArray UriList = new JSONArray();
            for (Group group : user.getGroupList()) {
                JSONObject o = new JSONObject();
                o.put("name", group.getName());
                o.put("uri", RestUriBuilder.getGroupUri(group));
                UriList.put(o);
            }
            return Response.ok(UriList.toString()).build();
        }
        finally {
            EMF.closeEntityManager(em);
        }
    }

    @POST
    @Consumes("application/json")
    @Produces("application/json")
    @Path("/groups")
    public Response postGroups(JSONObject groupData) throws Exception {

        if (!groupData.has("groupId")) {
            logger.error("User groups ID has to be provided.");
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        EntityManager em = EMF.createEntityManager();
        try {
            User user = em.find(User.class, userUuid);
            if (user == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }

            String groupString = groupData.getString("groupId");
            int groupId = Integer.parseInt(groupString.substring(groupString.lastIndexOf("/") + 1));
            logger.debug("Got Group Id: " + groupId);
            Group newgroup = em.find(Group.class, groupId);
            if (newgroup == null) {
                String message = String.format("Group with id %d doesn't exist.", groupId);
                return Response.status(Response.Status.BAD_REQUEST).entity(message).build();
            }

            em.getTransaction().begin();
            user.getGroupList().add(newgroup);
            newgroup.getUserList().add(user);
            em.getTransaction().commit();

            URI resourceUri = new URI(String.format("/%d", newgroup.getGroupId()));
            return Response.created(resourceUri).build();
        }
        finally {
            EMF.closeEntityManager(em);
        }
    }

    @DELETE
    @Path("/groups/{groupId}")
    public Response removeUserGroup(@PathParam("groupId") int groupId) throws Exception {
        EntityManager em = EMF.createEntityManager();
        try {
            User user = em.find(User.class, userUuid);
            if (user == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            Group group = em.find(Group.class, groupId);
            if (group == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }

            em.getTransaction().begin();
            user.getGroupList().remove(group);
            group.getUserList().remove(user);
            em.getTransaction().commit();

            return Response.status(Response.Status.NO_CONTENT).build();
        }
        finally {
            EMF.closeEntityManager(em);
        }
    }

    @GET
    @Consumes("application/json")
    @Produces("application/json")
    @Path("/dashboard")
    public Response getDashboard() throws Exception {
        logger.debug("Entering GET dashboard");

        EntityManager em = EMF.createEntityManager();
        try {
            User user = em.find(User.class, userUuid);
            if (user == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }

            JSONObject jsonDashboard = new JSONObject();

            JSONArray jsonTempArray = new JSONArray();
            for (Role role : user.getRoleList()) {
                JSONObject o = new JSONObject();
                o.put("name", role.getName());
                o.put("uri", RestUriBuilder.getRoleUri(role));
                jsonTempArray.put(o);
            }
            jsonDashboard.put("roles", jsonTempArray);

            jsonTempArray = new JSONArray();
            for (UserHasAttribute attribute : user.getUserHasAttributeList()) {
                JSONObject o = new JSONObject();
                o.put("name", attribute.getAttribute().getName());
                o.put("uri", RestUriBuilder.getUserAttrUri(attribute));
                jsonTempArray.put(o);
            }
            jsonDashboard.put("attributes", jsonTempArray);

            jsonTempArray = new JSONArray();
            for (Group group : user.getGroupList()) {
                JSONObject o = new JSONObject();
                o.put("name", group.getName());
                o.put("uri", RestUriBuilder.getGroupUri(group));
                jsonTempArray.put(o);
            }
            jsonDashboard.put("groups", jsonTempArray);

            jsonTempArray = new JSONArray();
            for (UserHasIdentityProvider userIdp : user.getUserHasIdentityProviderList()) {
                String uri = String.format("%s/ids/%s", RestUriBuilder.getUserUri(user),
                        userIdp.getIdentityProvider().getIdpId());
                JSONObject o = new JSONObject();
                o.put("identity", userIdp.getIdentity());
                o.put("uri", uri);
                jsonTempArray.put(o);
            }
            jsonDashboard.put("identities", jsonTempArray);


            return Response.ok(jsonDashboard.toString()).build();
        }
        finally {
            EMF.closeEntityManager(em);
        }
    }

    @GET
    @Path("oauth/access_tokens")
    public Response getAccessTokens(@Context HttpServletRequest httpServletRequest) throws Exception {
        return forwardToAS(String.format("/owners/%s/access_tokens", userUuid), httpServletRequest);
    }

    @GET
    @Path("oauth/access_tokens/{token}")
    public Response getAccessTokenInfo(@PathParam("token") String token,
                                       @Context HttpServletRequest httpServletRequest) throws Exception {
        return forwardToAS(String.format("/owners/%s/access_tokens/%s", userUuid, token), httpServletRequest);
    }

    @GET
    @Path("oauth/access_tokens/{token}/access_log")
    public Response getAccessTokenAccessLog(@PathParam("token") String token,
                                       @Context HttpServletRequest httpServletRequest) throws Exception {
        return forwardToAS(String.format("/owners/%s/access_tokens/%s/access_log", userUuid, token), httpServletRequest);
    }

    @GET
    @Path("oauth/access_log")
    public Response getAccessLog(@Context HttpServletRequest httpServletRequest) throws Exception {
        return forwardToAS(String.format("/owners/%s/access_log", userUuid), httpServletRequest);
    }

    @GET
    @Path("oauth/accesses_per_rs")
    public Response getAccessTokenInfo(@Context HttpServletRequest httpServletRequest) throws Exception {
        return forwardToAS(String.format("/owners/%s/accesses_per_rs", userUuid), httpServletRequest);
    }

    @GET
    @Path("oauth/trust/organizations")
    public Response getOrganizationsTrust(@Context HttpServletRequest httpServletRequest) throws Exception {
        return forwardToAS(String.format("/owners/%s/trust/organizations", userUuid), httpServletRequest);
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("oauth/trust/organizations")
    public Response addOrganizationTrust(@Context HttpServletRequest httpServletRequest) throws Exception {
        return forwardToAS(String.format("/owners/%s/trust/organizations", userUuid), httpServletRequest);
    }

    @GET
    @Path("oauth/trust/organizations/{organizationId}")
    public Response getOrganizationTrust(@PathParam("organizationId") int organizationId,
                                         @Context HttpServletRequest httpServletRequest) throws Exception {
        return forwardToAS(String.format("/owners/%s/trust/organizations/%d", userUuid, organizationId),
                httpServletRequest);
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("oauth/trust/organizations/{organizationId}")
    public Response updateOrganizationTrust(@PathParam("organizationId") int organizationId,
                                         @Context HttpServletRequest httpServletRequest) throws Exception {
        return forwardToAS(String.format("/owners/%s/trust/organizations/%d", userUuid, organizationId),
                httpServletRequest);
    }

    @DELETE
    @Path("oauth/trust/organizations/{orgId}")
    public Response deleteOrganizationTrust(@PathParam("orgId") int orgId,
                                         @Context HttpServletRequest httpServletRequest) throws Exception {
        return forwardToAS(String.format("/owners/%s/trust/organizations/%d", userUuid, orgId),
                httpServletRequest);
    }

    @GET
    @Path("oauth/trust/organizations/{orgId}/clients")
    public Response getClientsTrust(@PathParam("orgId") int orgId,
                                       @Context HttpServletRequest httpServletRequest) throws Exception {
        return forwardToAS(String.format("/owners/%s/trust/organizations/%d/clients", userUuid, orgId),
                httpServletRequest);
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("oauth/trust/organizations/{orgId}/clients")
    public Response addClientTrust(@PathParam("orgId") int orgId,
                                       @Context HttpServletRequest httpServletRequest) throws Exception {
        return forwardToAS(String.format("/owners/%s/trust/organizations/%d/clients", userUuid, orgId),
                httpServletRequest);
    }

    @GET
    @Path("oauth/trust/organizations/{orgId}/clients/{clientId}")
    public Response getClientTrust(@PathParam("orgId") int orgId, @PathParam("clientId") int clientId,
                                       @Context HttpServletRequest httpServletRequest) throws Exception {
        return forwardToAS(String.format("/owners/%s/trust/organizations/%d/clients/%d", userUuid, orgId, clientId),
                httpServletRequest);
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("oauth/trust/organizations/{orgId}/clients/{clientId}")
    public Response updateClientTrust(@PathParam("orgId") int orgId, @PathParam("clientId") int clientId,
                                       @Context HttpServletRequest httpServletRequest) throws Exception {
        return forwardToAS(String.format("/owners/%s/trust/organizations/%d/clients/%d", userUuid, orgId, clientId),
                httpServletRequest);
    }

    @DELETE
    @Path("oauth/trust/organizations/{orgId}/clients/{clientId}")
    public Response deleteClientTrust(@PathParam("orgId") int orgId, @PathParam("clientId") int clientId,
                                       @Context HttpServletRequest httpServletRequest) throws Exception {
        return forwardToAS(String.format("/owners/%s/trust/organizations/%d/clients/%d", userUuid, orgId, clientId),
                httpServletRequest);
    }

    private Response forwardToAS(String path, HttpServletRequest httpServletRequest) throws Exception {
        URI baseUri = Conf.getInstance().getAddressOAuthAS().resolve("admin/");
        Map<String, String> rewriteRules = new HashMap<String, String>();
        rewriteRules.put("\"\\/owners\\/" + userUuid, "\"/users/" + userUuid + "/oauth");

        Map<String, String> locationHeaderRewriteRules = new HashMap<String, String>();
        locationHeaderRewriteRules.put(baseUri + "owners/" + userUuid, "/users/" + userUuid + "/oauth");

        RestProxy restProxy = new RestProxy(baseUri, rewriteRules, locationHeaderRewriteRules);

        return restProxy.forward(path, httpServletRequest);
    }
}
