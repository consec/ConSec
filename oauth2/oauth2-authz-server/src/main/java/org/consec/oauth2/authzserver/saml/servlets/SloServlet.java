package org.consec.oauth2.authzserver.saml.servlets;

import org.apache.log4j.Logger;
import org.opensaml.common.SAMLObject;
import org.opensaml.common.binding.BasicSAMLMessageContext;
import org.opensaml.common.binding.SAMLMessageContext;
import org.opensaml.saml2.binding.decoding.HTTPRedirectDeflateDecoder;
import org.opensaml.saml2.core.*;
import org.opensaml.saml2.metadata.AssertionConsumerService;
import org.opensaml.saml2.metadata.EntityDescriptor;
import org.opensaml.saml2.metadata.SingleSignOnService;
import org.opensaml.saml2.metadata.provider.MetadataProvider;
import org.opensaml.ws.transport.http.HttpServletRequestAdapter;
import org.consec.oauth2.authzserver.common.Constants;
import org.consec.oauth2.authzserver.saml.OpenSAMLUtils;
import org.consec.oauth2.authzserver.saml.SAMLMessageBuilder;
import org.consec.oauth2.authzserver.saml.SAMLMetadata;
import org.consec.oauth2.authzserver.utils.Configuration;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import static org.opensaml.common.xml.SAMLConstants.SAML20P_NS;
import static org.opensaml.common.xml.SAMLConstants.SAML2_REDIRECT_BINDING_URI;

public class SloServlet extends HttpServlet {
    protected static Logger log = Logger.getLogger(SloServlet.class);

    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws javax.servlet.ServletException, java.io.IOException {
        try {
            process(request, response);
        }
        catch (Exception e) {
            log.error("Failed to process the Single logout request: " + e.getMessage(), e);
            throw new ServletException(e);
        }
    }

    private void process(HttpServletRequest request, HttpServletResponse response) throws Exception {
        log.trace("Single Logout Service: new message arrived.");
        log.trace("Trying to decode SAML logout request.");

        SAMLMessageContext context = decodeSAMLRequest(request);
        LogoutRequest logoutRequest = (LogoutRequest) context.getInboundSAMLMessage();

        log.trace("SAML Single Log-Out request decoded successfully.");
        log.trace("Message ID: " + logoutRequest.getID());
        log.trace("Issuer: " + logoutRequest.getIssuer().getValue());
        log.trace("Destination: " + logoutRequest.getDestination());
        log.trace("Issue instant: " + logoutRequest.getIssueInstant().toString());

        // destroy user HTTP session
        HttpSession session = request.getSession(false);
        if (session != null) {
            String ownerUuid = (String) session.getAttribute(Constants.SESSION_PARAM_OWNER_UUID);
            session.invalidate();
            log.trace(String.format("Session of the user '%s' has been invalidated.", ownerUuid));
        }
        else {
            log.trace("There is no active session. Nothing to invalidate.");
        }

        // create logout response
        log.trace("Creating SAML SLO response.");
        SAMLMessageBuilder builder = new SAMLMessageBuilder();
        Status status = builder.getStatus(StatusCode.SUCCESS_URI, null);
        builder.createLogoutResponse(status, context);

        // send response
        builder.encodeRequest(response, context);
        log.trace("SAML SLO response was sent successfully.");
        return;
    }

    private SAMLMessageContext decodeSAMLRequest(HttpServletRequest request) throws Exception {

        SAMLMetadata samlMetadata = Configuration.getInstance().getSAMLMetadata();
        MetadataProvider metadataProvider = samlMetadata.getMetadataProvider();

        SAMLMessageContext<Response, SAMLObject, NameID> context =
                new BasicSAMLMessageContext<Response, SAMLObject, NameID>();

        EntityDescriptor localEntity = metadataProvider.getEntityDescriptor(samlMetadata.getSpEntityId());
        OpenSAMLUtils.initializeLocalEntity(context, localEntity, localEntity.getSPSSODescriptor(SAML20P_NS),
                AssertionConsumerService.DEFAULT_ELEMENT_NAME);

        EntityDescriptor peerEntity = metadataProvider.getEntityDescriptor(samlMetadata.getIdpEntityId());
        OpenSAMLUtils.initializePeerEntity(context, peerEntity, peerEntity.getIDPSSODescriptor(SAML20P_NS),
                SingleSignOnService.DEFAULT_ELEMENT_NAME,
                SAML2_REDIRECT_BINDING_URI);

        context.setInboundMessageTransport(new HttpServletRequestAdapter(request));
        HTTPRedirectDeflateDecoder decoder = new HTTPRedirectDeflateDecoder();
        decoder.decode(context);

        // TODO: check logout request (SingleLogoutProfileImpl.processLogoutRequest)

        return context;
    }
}

