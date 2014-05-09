package org.consec.oauth2.authzserver.endpoints;

import org.apache.amber.oauth2.as.issuer.OAuthIssuer;
import org.apache.amber.oauth2.as.issuer.OAuthIssuerImpl;
import org.apache.amber.oauth2.as.issuer.UUIDValueGenerator;
import org.apache.amber.oauth2.as.request.OAuthTokenRequest;
import org.apache.amber.oauth2.as.response.OAuthASResponse;
import org.apache.amber.oauth2.common.error.OAuthError;
import org.apache.amber.oauth2.common.exception.OAuthProblemException;
import org.apache.amber.oauth2.common.exception.OAuthSystemException;
import org.apache.amber.oauth2.common.message.OAuthResponse;
import org.apache.amber.oauth2.common.message.types.GrantType;
import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.consec.oauth2.authzserver.jpa.dao.AccessTokenDao;
import org.consec.oauth2.authzserver.jpa.dao.AuthzCodeDao;
import org.consec.oauth2.authzserver.jpa.dao.ClientDao;
import org.consec.oauth2.authzserver.jpa.dao.OwnerDao;
import org.consec.oauth2.authzserver.jpa.entities.*;
import org.consec.oauth2.authzserver.jpa.enums.AuthorizedGrantType;
import org.consec.oauth2.authzserver.utils.AmberHttpServletRequest;
import org.consec.oauth2.authzserver.utils.Configuration;
import org.consec.oauth2.authzserver.utils.PersistenceUtils;
import org.consec.oauth2.authzserver.utils.TrustUtils;

import javax.persistence.EntityManager;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.security.cert.X509Certificate;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Path("/access_token")
public class TokenEndpoint {
    protected static Logger log = Logger.getLogger(TokenEndpoint.class);
    private static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

    @Context
    private HttpServletRequest httpRequest;

    @POST
    @Consumes("application/x-www-form-urlencoded")
    @Produces("application/json")
    @Path("/request")
    public Response getToken(MultivaluedMap<String, String> params) throws OAuthSystemException {
        log.trace("Received OAuth access token request: " + params.toString());

        OAuthTokenRequest oauthRequest = null;
        try {
            log.trace("Parsing access token request...");
            AmberHttpServletRequest request = new AmberHttpServletRequest(params, httpRequest.getMethod(),
                    httpRequest.getContentType());
            oauthRequest = new OAuthTokenRequest(request);
            log.trace("Request was parsed successfully.");
        }
        catch (OAuthProblemException e) {
            log.trace("Request is invalid: " + e.getMessage());
            OAuthResponse response = OAuthASResponse.errorResponse(HttpServletResponse.SC_BAD_REQUEST)
                    .setError(e.getError()).setErrorDescription(e.getDescription())
                    .buildJSONMessage();
            return Response.status(response.getResponseStatus()).entity(response.getBody()).build();
        }

        if (oauthRequest.getGrantType().equals(GrantType.AUTHORIZATION_CODE.toString())) {
            log.trace("Request grant type is AUTHORIZATION_CODE.");
            return processAuthCodeGrant(oauthRequest, params);
        }
        else if (oauthRequest.getGrantType().equals(GrantType.CLIENT_CREDENTIALS.toString())) {
            log.trace("Request grant type is CLIENT_CREDENTIALS.");
            return processClientCredentialsGrant(oauthRequest, httpRequest.getHeader("Authorization"));
        }
        else {
            log.trace("Unsupported grant type: " + oauthRequest.getGrantType());
            OAuthResponse oauthResponse = OAuthASResponse.errorResponse(HttpServletResponse.SC_BAD_REQUEST)
                    .setError(OAuthError.TokenResponse.UNSUPPORTED_GRANT_TYPE)
                    .setErrorDescription(String.format("Grant type %s is not supported.", oauthRequest.getGrantType()))
                    .buildJSONMessage();
            return Response.status(oauthResponse.getResponseStatus()).entity(oauthResponse.getBody()).build();
        }
    }

    private Response processAuthCodeGrant(OAuthTokenRequest oauthRequest, MultivaluedMap<String, String> params)
            throws OAuthSystemException {
        EntityManager em = PersistenceUtils.getInstance().getEntityManager();
        try {
            log.trace("Validating access token request...");

            Client client = new ClientDao(em).findByClientId(oauthRequest.getClientId());
            if (client == null) {
                throw OAuthProblemException.error(OAuthError.TokenResponse.INVALID_CLIENT, "Client_id not found.");
            }

            if (!client.getClientSecret().equals(oauthRequest.getClientSecret())) {
                throw OAuthProblemException.error(OAuthError.TokenResponse.INVALID_CLIENT, "Invalid client secret.");
            }

            AuthzCode authzCode = new AuthzCodeDao(em).findByCode(oauthRequest.getCode());
            if (authzCode == null) {
                throw OAuthProblemException.error(OAuthError.TokenResponse.INVALID_GRANT, "Invalid authorization code.");
            }

            if (!authzCode.getClient().equals(client)) {
                throw OAuthProblemException.error(OAuthError.TokenResponse.INVALID_GRANT, "Client mismatch.");
            }

            if (authzCode.getRedirectUri() != null &&
                    !authzCode.getRedirectUri().equals(oauthRequest.getRedirectURI())) {
                throw OAuthProblemException.error(OAuthError.TokenResponse.INVALID_GRANT, "Invalid redirect_uri.");
            }

            Date now = new Date();
            if (authzCode.getExpireTime().before(now)) {
                throw OAuthProblemException.error(OAuthError.TokenResponse.INVALID_GRANT,
                        "Authorization code has expired.");
            }

            if (!compareScopes(authzCode.getScope(), params.getFirst("scope"))) {
                throw OAuthProblemException.error(OAuthError.TokenResponse.INVALID_GRANT, "Scope mismatch.");
            }

            log.trace("Access token request is valid.");

            log.trace("Issuing access token based on authorization code " + authzCode.getCode());
            OAuthIssuer oauthIssuerImpl = new OAuthIssuerImpl(new UUIDValueGenerator());
            String token = oauthIssuerImpl.accessToken();

            int timeout = Configuration.getInstance().getAccessTokenTimeout();
            Timestamp expireTime = new Timestamp(now.getTime() + timeout * 1000);

            AccessToken accessToken = new AccessToken();
            accessToken.setToken(token);
            accessToken.setExpireTime(expireTime);
            accessToken.setClient(client);
            accessToken.setOwner(authzCode.getOwner());
            accessToken.setScope(authzCode.getScope());

            em.getTransaction().begin();
            em.persist(accessToken);
            em.remove(authzCode);
            em.getTransaction().commit();

            OAuthResponse oauthResponse = OAuthASResponse
                    .tokenResponse(HttpServletResponse.SC_OK)
                    .setAccessToken(accessToken.getToken())
                    .setExpiresIn(Integer.toString(timeout))
                    .buildJSONMessage();

            log.trace("Access token created successfully: " + token);
            log.trace("Returning response: " + oauthResponse.getBody());
            return Response.status(oauthResponse.getResponseStatus()).entity(oauthResponse.getBody()).build();
        }
        catch (OAuthProblemException e) {
            log.trace("Access token request is invalid: " + e.getMessage());
            OAuthResponse oauthResponse = OAuthASResponse.errorResponse(HttpServletResponse.SC_BAD_REQUEST)
                    .setError(e.getError()).setErrorDescription(e.getDescription())
                    .buildJSONMessage();
            return Response.status(oauthResponse.getResponseStatus()).entity(oauthResponse.getBody()).build();
        }
        catch (Exception e) {
            log.error("Failed to process the access token request: " + e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
        }
        finally {
            if (em != null) {
                PersistenceUtils.getInstance().closeEntityManager(em);
            }
        }
    }

    private Response processClientCredentialsGrant(OAuthTokenRequest oauthRequest, String authHeader)
            throws OAuthSystemException {
        EntityManager em = PersistenceUtils.getInstance().getEntityManager();
        try {

            if (authHeader == null) {
                log.trace("Missing Authorization header.");
                throw OAuthProblemException.error(OAuthError.TokenResponse.UNAUTHORIZED_CLIENT);
            }

            Pattern pattern = Pattern.compile("Basic (\\S+)");
            Matcher m = pattern.matcher(authHeader);
            if (!m.find()) {
                log.trace("Invalid Authorization header.");
                throw OAuthProblemException.error(OAuthError.TokenResponse.UNAUTHORIZED_CLIENT);
            }

            byte[] bytes = Base64.decodeBase64(m.group(1).getBytes());
            String credentials = new String(bytes);
            String[] credentialsArr = credentials.split(":");
            if (credentialsArr == null || credentialsArr.length != 2) {
                log.trace("Invalid Authorization header.");
                throw OAuthProblemException.error(OAuthError.TokenResponse.UNAUTHORIZED_CLIENT);
            }
            String clientId = credentialsArr[0];
            String clientSecret = credentialsArr[1];

            if (log.isTraceEnabled()) {
                log.trace(String.format("Client credentials request info: clientId=%s, resource_owner=%s",
                        clientId,
                        oauthRequest.getParam("resource_owner")));
            }

            Client client = new ClientDao(em).findByClientId(clientId);
            if (client == null) {
                log.trace("No such client: " + clientId);
                throw OAuthProblemException.error(OAuthError.TokenResponse.UNAUTHORIZED_CLIENT);
            }

            if (!client.getClientSecret().equals(clientSecret)) {
                log.trace("Invalid client secret.");
                throw OAuthProblemException.error(OAuthError.TokenResponse.INVALID_CLIENT, "Invalid client secret.");
            }

            if (!client.getAuthorizedGrantTypes().contains(AuthorizedGrantType.CLIENT_CREDENTIALS)) {
                log.trace("Client credentials grant type is not allowed for this client.");
                throw OAuthProblemException.error(OAuthError.TokenResponse.UNAUTHORIZED_CLIENT,
                        String.format("Client %s is not allowed to get client access token.", clientId));
            }

            // resource owner
            String resourceOwner = oauthRequest.getParam("resource_owner");
            if (resourceOwner == null) {
                log.trace("Missing parameter resource_owner.");
                throw OAuthProblemException.error(OAuthError.TokenResponse.INVALID_REQUEST,
                        "Missing parameter resource_owner.");
            }
            Owner owner = new OwnerDao(em).findByUuid(resourceOwner);
            if (owner == null) {
                log.trace("No such resource owner: " + resourceOwner);
                throw OAuthProblemException.error(OAuthError.TokenResponse.INVALID_REQUEST,
                        "Unknown resource owner: '" + resourceOwner + "'");
            }

            // check if the client is trusted by the owner
            // TODO: currently only FULL_ACCESS scope is supported. Actual scope should be taken from the request.
            TrustUtils trustUtils = new TrustUtils(em);
            Boolean isTrusted = trustUtils.isTrusted(owner, client);
            if (isTrusted == null) {
                String message = String.format("The resource owner hasn't given consent for client %s to access " +
                        "his protected resources (the client is not trusted).", client.getClientId());
                log.trace(message);
                return Response.status(Response.Status.FORBIDDEN).entity(message).build();
            }
            else if (!isTrusted) {
                String message = String.format("The resource owner denied the client %s to access his protected " +
                        "resources.", client.getClientId());
                log.trace(message);
                return Response.status(Response.Status.FORBIDDEN).entity(message).build();
            }

            // issue client access token
            OAuthIssuer oauthIssuerImpl = new OAuthIssuerImpl(new UUIDValueGenerator());
            String token = oauthIssuerImpl.accessToken();

            int timeout = Configuration.getInstance().getAccessTokenTimeout();
            Date now = new Date();
            Timestamp expireTime = new Timestamp(now.getTime() + timeout * 1000);

            AccessToken accessToken = new AccessToken();
            accessToken.setToken(token);
            accessToken.setExpireTime(expireTime);
            accessToken.setClient(client);
            accessToken.setOwner(owner);

            em.getTransaction().begin();
            em.persist(accessToken);
            em.getTransaction().commit();

            OAuthResponse oauthResponse = OAuthASResponse
                    .tokenResponse(HttpServletResponse.SC_OK)
                    .setAccessToken(accessToken.getToken())
                    .setExpiresIn(Integer.toString(timeout))
                    .buildJSONMessage();

            if (log.isTraceEnabled()) {
                log.trace("Returning access token: " + accessToken.toJson().toString());
            }

            return Response.status(oauthResponse.getResponseStatus()).entity(oauthResponse.getBody()).build();
        }
        catch (OAuthProblemException e) {
            log.trace(String.format("Access token request denied: %s %s", e.getError(), e.getDescription()));
            OAuthResponse oauthResponse = OAuthASResponse.errorResponse(HttpServletResponse.SC_BAD_REQUEST)
                    .setError(e.getError()).setErrorDescription(e.getDescription())
                    .buildJSONMessage();
            return Response.status(oauthResponse.getResponseStatus()).entity(oauthResponse.getBody()).build();
        }
        catch (Exception e) {
            log.error("Failed to process the access token request: " + e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
        }
        finally {
            if (em != null) {
                PersistenceUtils.getInstance().closeEntityManager(em);
            }
        }
    }

    @POST
    @Consumes("application/x-www-form-urlencoded")
    @Produces("application/json")
    @Path("/check")
    public Response validateToken(MultivaluedMap<String, String> formParams) {
        if (!formParams.containsKey("access_token")) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Missing parameter 'access_token'.").build();
        }
        if (!formParams.containsKey("bearer_id")) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Missing parameter 'bearer_id'.").build();
        }

        X509Certificate[] certs = (X509Certificate[]) httpRequest.getAttribute("javax.servlet.request.X509Certificate");
        if (certs == null || certs.length == 0) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("The client SSL certificate was not provided.").build();
        }
        String resourceServerName = certs[0].getSubjectDN().getName();

        String token = formParams.getFirst("access_token");
        String bearerDN = formParams.getFirst("bearer_id");

        // determine token bearer based on its client_id
        Pattern pattern = Pattern.compile("CN=([^,]+)");
        Matcher m = pattern.matcher(bearerDN);
        if (!m.find()) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity("Invalid bearer_id: " + bearerDN).build();
        }
        String bearerClientId = m.group(1);

        if (log.isTraceEnabled()) {
            log.trace(String.format("validateToken() called: access_token='%s', bearer_name='%s', " +
                    "resource_server_name='%s'", token, bearerDN, resourceServerName));
        }

        EntityManager em = PersistenceUtils.getInstance().getEntityManager();
        try {
            log.trace("Checking access token validity...");
            AccessToken accessToken = new AccessTokenDao(em).findByToken(token);
            JSONObject result = new JSONObject();
            Date now = new Date();

            // check if access token is valid
            if (accessToken == null ||
                    accessToken.getExpireTime().before(now) ||
                    accessToken.isRevoked()) {
                log.trace("Access token is invalid. Returning status code UNAUTHORIZED.");
                return Response.status(Response.Status.UNAUTHORIZED).build();
            }

            // check if bearer is trusted
            Client bearer = new ClientDao(em).findByClientId(bearerClientId);
            if (bearer == null) {
                String message = String.format("Invalid bearer_id '%s': no such client registered.", bearerDN);
                log.trace(message);
                return Response.status(Response.Status.UNAUTHORIZED).entity(message).build();
            }
            TrustUtils trustUtils = new TrustUtils(em);
            Boolean isTrusted = trustUtils.isTrusted(accessToken.getOwner(), bearer);
            if (isTrusted == null) {
                String message = String.format(
                        "User's %s trust is not defined for the bearer of the access token '%s'.",
                        accessToken.getOwner().getUuid(), bearerDN);
                log.trace(message);
                return Response.status(Response.Status.UNAUTHORIZED).entity(message).build();
            }
            else if (!isTrusted) {
                String message = String.format("Bearer of the access token '%s' is not trusted by the user %s.",
                        bearerDN, accessToken.getOwner().getUuid());
                log.trace(message);
                return Response.status(Response.Status.UNAUTHORIZED).entity(message).build();
            }

            log.trace("Access token is valid. Returning token info.");
            long expiresIn = (accessToken.getExpireTime().getTime() - now.getTime()) / 1000;
            result.put("access_token", accessToken.getToken());
            result.put("client_id", accessToken.getClient().getClientId());
            result.put("expire_time", sdf.format(accessToken.getExpireTime()));
            result.put("expires_in", expiresIn);
            result.put("token_type", "Bearer");
            result.put("owner_uuid", accessToken.getOwner().getUuid());

            JSONArray scopeArr = new JSONArray();
            if (accessToken.getScope() != null) {
                String[] scopeNames = accessToken.getScope().split(" ");
                for (String scopeName : scopeNames) {
                    scopeArr.put(scopeName);
                }
            }
            result.put("scope", scopeArr);

            // store access request to token-info access log
            TokenInfoAccessLog accessLog = new TokenInfoAccessLog();
            accessLog.setAccessToken(accessToken);
            accessLog.setBearerName(bearerDN);
            accessLog.setResourceServerName(resourceServerName);
            accessLog.setTimestamp(new Date());
            em.getTransaction().begin();
            em.persist(accessLog);
            em.getTransaction().commit();

            return Response.ok().entity(result.toString()).build();
        }
        catch (Exception e) {
            log.error("Failed to validate access token: " + e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
        }
        finally {
            PersistenceUtils.getInstance().closeEntityManager(em);
        }
    }

    private boolean compareScopes(String scope1, String scope2) {
        if (scope1 == null && scope2 == null) {
            return true;
        }
        else if (scope1 == null || scope2 == null) {
            return false;
        }
        else {
            return scope1.equals(scope2);
        }
    }
}
