package org.consec.oauth2.authzserver.saml.servlets;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.opensaml.common.SAMLObject;
import org.opensaml.common.binding.BasicSAMLMessageContext;
import org.opensaml.common.binding.SAMLMessageContext;
import org.opensaml.saml2.binding.decoding.HTTPPostDecoder;
import org.opensaml.saml2.core.*;
import org.opensaml.saml2.metadata.AssertionConsumerService;
import org.opensaml.saml2.metadata.EntityDescriptor;
import org.opensaml.saml2.metadata.SingleSignOnService;
import org.opensaml.saml2.metadata.provider.MetadataProvider;
import org.opensaml.ws.transport.http.HttpServletRequestAdapter;
import org.consec.oauth2.authzserver.common.Constants;
import org.consec.oauth2.authzserver.jpa.dao.OwnerDao;
import org.consec.oauth2.authzserver.jpa.entities.Owner;
import org.consec.oauth2.authzserver.saml.OpenSAMLUtils;
import org.consec.oauth2.authzserver.saml.SAMLMetadata;
import org.consec.oauth2.authzserver.utils.Configuration;
import org.consec.oauth2.authzserver.utils.PersistenceUtils;

import javax.persistence.EntityManager;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.opensaml.common.xml.SAMLConstants.SAML20P_NS;
import static org.opensaml.common.xml.SAMLConstants.SAML2_REDIRECT_BINDING_URI;

public class AcsServlet extends HttpServlet {
    protected static Logger log = Logger.getLogger(AcsServlet.class);

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

    private void process(HttpServletRequest request, HttpServletResponse response) throws Exception {
        log.trace("Assertion Consumer Service: new message arrived.");
        log.trace("Trying to decode SAML response.");
        Response samlResponse = decodePostResponse(request);
        log.trace("Message decoded successfully.");
        log.trace("Message ID: " + samlResponse.getID());
        log.trace("InResponseTo: " + samlResponse.getInResponseTo());
        log.trace("Issuer: " + samlResponse.getIssuer().getValue());

        String code = samlResponse.getStatus().getStatusCode().getValue();
        log.trace("Status code: " + code);
        if (code.equals(StatusCode.SUCCESS_URI)) {
            processSAMLResponse(request, response, samlResponse);
        }
        else if (code.equals(StatusCode.AUTHN_FAILED_URI)) {
            throw new Exception("Authentication failed.");
        }
        else {
            throw new Exception("Unexpected SAML response received with status code " + code);
        }
    }

    private void processSAMLResponse(HttpServletRequest request, HttpServletResponse response,
                                     Response samlResponse) throws Exception {
        List<Assertion> assertions = samlResponse.getAssertions();
        if (assertions.size() == 0) {
            throw new Exception("Invalid SAML response received: no assertions found.");
        }
        Assertion assertion = assertions.get(0);

        log.trace("Parsing attributes in SAML assertion.");
        Map<String, Object> attrs = new HashMap<String, Object>();
        try {
            List<Attribute> attributeList = assertion.getAttributeStatements().get(0).getAttributes();
            for (Attribute attribute : attributeList) {
                String name = attribute.getName();
                try {
                    if (attribute.getAttributeValues() == null || attribute.getAttributeValues().size() == 0) {
                        log.trace(String.format("Attribute %s has no value.", name));
                        attrs.put(name, null);
                    }
                    else if (attribute.getAttributeValues().size() == 1) {
                        String value = attribute.getAttributeValues().get(0).getDOM().getTextContent();
                        attrs.put(name, value);
                    }
                    else {
                        List<String> values = new ArrayList<String>();
                        for (int i = 0; i < attribute.getAttributeValues().size(); i++) {
                            String value = attribute.getAttributeValues().get(i).getDOM().getTextContent();
                            values.add(value);
                        }
                        attrs.put(name, values);
                    }
                    log.trace(String.format("Attribute %s parsed successfully.", name));
                }
                catch (Exception e) {
                    log.error(String.format(
                            "Failed to parse attribute %s in SAML assertion. The attribute will be ignored.", name));
                }
            }
            log.trace("Attributes in the SAML assertion: " + attrs.toString());
        }
        catch (Exception e) {
            throw new Exception("Failed to parse attributes from SAML response: " + e.getMessage(), e);
        }

        JSONObject attrsMapping = Configuration.getInstance().getSAMLAttrsMapping();
        if (log.isTraceEnabled()) {
            log.trace("Attributes mapping: " + attrsMapping.toString());
        }

        String userUuid = (String) attrs.get(attrsMapping.getString("uuid"));

        if (userUuid == null) {
            throw new Exception("Invalid SAML response received: compulsory attributes missing.");
        }

        log.trace("User UUID: " + userUuid);
        EntityManager em = PersistenceUtils.getInstance().getEntityManager();
        Owner owner;
        try {
            owner = new OwnerDao(em).getOwner(userUuid);
        }
        finally {
            PersistenceUtils.getInstance().closeEntityManager(em);
        }

        log.trace("Storing authentication data to user session.");
        HttpSession session = request.getSession();
        session.setAttribute(Constants.SESSION_PARAM_AUTHENTICATED, true);
        session.setAttribute(Constants.SESSION_PARAM_OWNER_UUID, owner.getUuid());

        String returnTo = request.getParameter("RelayState");
        log.trace("Forwarding request to " + returnTo);
        RequestDispatcher dispatcher = request.getRequestDispatcher(returnTo);
        dispatcher.forward(request, response);
    }

    private Response decodePostResponse(HttpServletRequest request) throws Exception {

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
        HTTPPostDecoder decoder = new HTTPPostDecoder();
        decoder.decode(context);

        Response samlResponse = context.getInboundSAMLMessage();
        if (samlResponse == null) {
            throw new IOException("Decoded SAML response is null.");
        }

        return samlResponse;
    }
}
