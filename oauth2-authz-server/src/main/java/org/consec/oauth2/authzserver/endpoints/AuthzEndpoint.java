package org.consec.oauth2.authzserver.endpoints;

import org.apache.amber.oauth2.as.issuer.MD5Generator;
import org.apache.amber.oauth2.as.issuer.OAuthIssuerImpl;
import org.apache.amber.oauth2.as.request.OAuthAuthzRequest;
import org.apache.amber.oauth2.as.response.OAuthASResponse;
import org.apache.amber.oauth2.common.OAuth;
import org.apache.amber.oauth2.common.error.OAuthError;
import org.apache.amber.oauth2.common.exception.OAuthProblemException;
import org.apache.amber.oauth2.common.exception.OAuthSystemException;
import org.apache.amber.oauth2.common.message.OAuthResponse;
import org.apache.amber.oauth2.common.message.types.ResponseType;
import org.apache.amber.oauth2.common.utils.OAuthUtils;
import org.apache.log4j.Logger;
import org.opensaml.common.SAMLException;
import org.opensaml.common.binding.SAMLMessageContext;
import org.opensaml.saml2.core.impl.AuthnRequestImpl;
import org.consec.oauth2.authzserver.common.Constants;
import org.consec.oauth2.authzserver.jpa.dao.ClientDao;
import org.consec.oauth2.authzserver.jpa.dao.OwnerDao;
import org.consec.oauth2.authzserver.jpa.entities.AuthzCode;
import org.consec.oauth2.authzserver.jpa.entities.Client;
import org.consec.oauth2.authzserver.jpa.entities.Owner;
import org.consec.oauth2.authzserver.jpa.enums.ClientTrustLevel;
import org.consec.oauth2.authzserver.saml.SAMLMessageBuilder;
import org.consec.oauth2.authzserver.utils.Configuration;
import org.consec.oauth2.authzserver.utils.PersistenceUtils;
import org.consec.oauth2.authzserver.utils.TrustUtils;

import javax.persistence.EntityManager;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.Set;

public class AuthzEndpoint extends HttpServlet {
    protected static Logger log = Logger.getLogger(AuthzEndpoint.class);

    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws javax.servlet.ServletException, java.io.IOException {
        try {
            process(request, response);
        }
        catch (Exception e) {
            log.error("Failed to process the request: " + e.getMessage(), e);
            throw new ServletException(e);
        }
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws javax.servlet.ServletException, java.io.IOException {
        try {
            process(request, response);
        }
        catch (Exception e) {
            log.error("Failed to process the request: " + e.getMessage(), e);
            throw new ServletException(e);
        }
    }

    private void process(HttpServletRequest request, HttpServletResponse response)
            throws IOException, OAuthSystemException, SAMLException {

        if (log.isTraceEnabled()) {
            log.trace("Received OAuth authorization request: " + request.getRequestURI() + "?" +
                    request.getQueryString());
        }

        // check if user is authenticated
        HttpSession session = request.getSession();
        Boolean authenticated = (Boolean) session.getAttribute(Constants.SESSION_PARAM_AUTHENTICATED);
        if (!Boolean.TRUE.equals(authenticated)) {
            log.trace("User is not authenticated.");

            // generate SAML authentication request and redirect user to the Identity Provider
            log.trace("Trying to authenticate user using SAML.");
            try {
                log.trace("Generating SAML authentication request.");
                String returnTo = request.getServletPath() + "?" + request.getQueryString();
                SAMLMessageBuilder builder = new SAMLMessageBuilder();
                SAMLMessageContext context = builder.createAuthnRequest(returnTo);
                if (log.isTraceEnabled()) {
                    log.trace("Using SAML Identity Provider " + context.getPeerEntityId());
                    log.trace("SAML request ID: " + ((AuthnRequestImpl) context.getOutboundSAMLMessage()).getID());
                    log.trace("Redirecting user to the IdP SSO service " +
                            context.getPeerEntityEndpoint().getLocation());
                }
                builder.encodeRequest(response, context);
                return;
            }
            catch (Exception e) {
            	log.error("Failed to generate SAML authentication request.", e);
                throw new SAMLException("Failed to generate SAML authentication request.");
            }
        }
        else {
            log.trace("User session is valid.");
        }

        String ownerUuid = (String) session.getAttribute(Constants.SESSION_PARAM_OWNER_UUID);
        log.trace("User UUID: " + ownerUuid);

        // parse OAuth authorization request
        OAuthAuthzRequest oauthRequest = null;
        log.trace("Parsing OAuth authorization request...");
        try {
            oauthRequest = new OAuthAuthzRequest(request);
            log.trace("Authorization request was parsed successfully.");
        }
        catch (OAuthProblemException e) {
            log.trace("Authorization request is invalid: " + e.getMessage());
            String redirectUri = e.getRedirectUri();
            if (OAuthUtils.isEmpty(redirectUri)) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
                return;
            }
            else {
                OAuthResponse oauthResponse = OAuthASResponse.errorResponse(HttpServletResponse.SC_FOUND)
                        .error(e)
                        .location(redirectUri).buildQueryMessage();
                response.sendRedirect(oauthResponse.getLocationUri());
                return;
            }
        }

        String redirectURI = oauthRequest.getRedirectURI();
        String state = oauthRequest.getState();
        Set<String> scopes = oauthRequest.getScopes();
        String consent = request.getParameter("consent");
        EntityManager em = null;

        try {
            em = PersistenceUtils.getInstance().getEntityManager();

            Owner owner = new OwnerDao(em).findByUuid(ownerUuid);

            log.trace("Validating OAuth authorization request...");
            // client_id
            Client client = new ClientDao(em).findByClientId(oauthRequest.getClientId());
            if (client == null) {
                throw OAuthProblemException.error(OAuthError.CodeResponse.UNAUTHORIZED_CLIENT, "Invalid client ID.");
            }

            // redirect_uri
            if (redirectURI == null) {
                redirectURI = client.getCallbackUri();
            }

            if (!redirectURI.startsWith(client.getCallbackUri())) {
                throw OAuthProblemException.error("redirect_uri_mismatch");
            }

            // response_type
            String responseType = oauthRequest.getParam(OAuth.OAUTH_RESPONSE_TYPE);
            if (!responseType.equals(ResponseType.CODE.toString())) {
                throw OAuthProblemException.error(OAuthError.CodeResponse.UNSUPPORTED_RESPONSE_TYPE);
            }

            log.trace("Authorization request is valid.");

            TrustUtils trustUtils = new TrustUtils(em);

            if (consent == null) {

                log.trace("Checking if client is authorized to access specified owner's resources...");
                Boolean isAuthorized = trustUtils.isTrusted(owner, client);

                if (isAuthorized == null) {
                    log.trace("Owner hasn't given the consent yet (for at least one requested resources). " +
                            "We have to ask user for consent.");
                    log.trace("Forwarding request to confirm.jsp");
                    RequestDispatcher rd = request.getRequestDispatcher("confirm.jsp");
                    request.setAttribute("client", client);
                    String returnTo = request.getRequestURL().toString() + "?" + request.getQueryString();
                    request.setAttribute("return_to", returnTo);
                    request.setAttribute("scopes", scopes);
                    rd.forward(request, response);
                    return;
                }
                else if (isAuthorized.equals(false)) {
                    log.trace("Resource owner already denied access to one or more requested resources. Returning " +
                            "ACCESS_DENIED.");
                    throw OAuthProblemException.error(OAuthError.CodeResponse.ACCESS_DENIED);
                }
                else {
                    log.trace("Resource owner already allowed access to all specified resources.");
                }
            }
            else if (!consent.equals("Allow access")) {
                log.trace("Resource owner denied access. Returning ACCESS_DENIED.");
                String justThisTime = request.getParameter("justThisTime");
                if (! "true".equals(justThisTime)) {
                    trustUtils.setClientTrust(owner, client, ClientTrustLevel.NOT_TRUSTED);
                }

                throw OAuthProblemException.error(OAuthError.CodeResponse.ACCESS_DENIED);
            }
            else {
                String justThisTime = request.getParameter("justThisTime");
                if (! "true".equals(justThisTime)) {
                    trustUtils.setClientTrust(owner, client, ClientTrustLevel.TRUSTED);
                }
                log.trace("Resource owner allowed access to specified resources.");
            }

            log.trace("Issuing authorization code...");
            OAuthIssuerImpl oauthIssuerImpl = new OAuthIssuerImpl(new MD5Generator());
            String code = oauthIssuerImpl.authorizationCode();

            AuthzCode authzCode = new AuthzCode();
            authzCode.setCode(code);
            authzCode.setClient(client);
            authzCode.setRedirectUri(redirectURI);
            authzCode.setOwner(owner);
            authzCode.setScope(request.getParameter("scope"));

            int timeout = Configuration.getInstance().getAuthzCodeTimeout();
            Date now = new Date();
            Timestamp expireTime = new Timestamp(now.getTime() + timeout * 1000);
            authzCode.setExpireTime(expireTime);

            em.getTransaction().begin();
            em.persist(authzCode);
            em.getTransaction().commit();
            log.trace("Authorization code was created successfully: " + code);

            OAuthASResponse.OAuthAuthorizationResponseBuilder builder = OAuthASResponse
                    .authorizationResponse(request, HttpServletResponse.SC_FOUND);
            builder.setCode(code);

            OAuthResponse oauthResponse = builder.location(redirectURI).buildQueryMessage();
            response.setStatus(oauthResponse.getResponseStatus());
            log.trace("Redirecting user to " + oauthResponse.getLocationUri());
            response.sendRedirect(oauthResponse.getLocationUri());
            return;
        }
        catch (OAuthProblemException e) {
            log.trace("Authorization request is invalid: " + e.getMessage());
            if (redirectURI != null) {
                OAuthResponse oauthResponse = OAuthASResponse.errorResponse(HttpServletResponse.SC_FOUND)
                        .location(redirectURI)
                        .setState(state)
                        .setError(e.getError())
                        .setErrorDescription(e.getDescription())
                        .buildQueryMessage();
                response.sendRedirect(oauthResponse.getLocationUri());
            }
            else {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
            }
        }
        catch (Exception e) {
            log.error("Failed to process the authorization request: " + e.getMessage(), e);
            if (redirectURI != null) {
                OAuthResponse oauthResponse = OAuthASResponse.errorResponse(HttpServletResponse.SC_INTERNAL_SERVER_ERROR)
                        .location(redirectURI)
                        .setState(state)
                        .setError(OAuthError.CodeResponse.SERVER_ERROR)
                        .setErrorDescription(e.getMessage())
                        .buildQueryMessage();
                response.sendRedirect(oauthResponse.getLocationUri());
            }
            else {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
            }
        }
        finally {
            PersistenceUtils.getInstance().closeEntityManager(em);
        }
    }
}
