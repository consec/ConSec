package org.consec.oauth2.authzserver.saml;

import org.joda.time.DateTime;
import org.opensaml.common.*;
import org.opensaml.common.binding.BasicSAMLMessageContext;
import org.opensaml.common.binding.SAMLMessageContext;
import org.opensaml.common.impl.SecureRandomIdentifierGenerator;
import org.opensaml.saml2.binding.encoding.HTTPRedirectDeflateEncoder;
import org.opensaml.saml2.core.*;
import org.opensaml.saml2.metadata.*;
import org.opensaml.saml2.metadata.provider.MetadataProvider;
import org.opensaml.saml2.metadata.provider.MetadataProviderException;
import org.opensaml.ws.message.encoder.MessageEncoder;
import org.opensaml.ws.message.encoder.MessageEncodingException;
import org.opensaml.ws.transport.http.HttpServletResponseAdapter;
import org.opensaml.xml.XMLObjectBuilderFactory;
import org.consec.oauth2.authzserver.utils.Configuration;

import javax.servlet.http.HttpServletResponse;
import javax.xml.namespace.QName;
import java.security.NoSuchAlgorithmException;

import static org.opensaml.common.xml.SAMLConstants.SAML20P_NS;
import static org.opensaml.common.xml.SAMLConstants.SAML2_REDIRECT_BINDING_URI;

public class SAMLMessageBuilder {

    private static final XMLObjectBuilderFactory objectBuilderFactory =
            org.opensaml.Configuration.getBuilderFactory();

    private static final SAMLObjectBuilder<AuthnRequest> authnRequestBuilder =
            makeSamlObjectBuilder(AuthnRequest.DEFAULT_ELEMENT_NAME);

    private static final SAMLObjectBuilder<Issuer> issuerBuilder =
            makeSamlObjectBuilder(Issuer.DEFAULT_ELEMENT_NAME);

    private static final SAMLObjectBuilder<NameIDPolicy> nameIdPolicyBuilder =
            makeSamlObjectBuilder(NameIDPolicy.DEFAULT_ELEMENT_NAME);

    private static final SAMLObjectBuilder<LogoutResponse> logoutResponseBuilder =
            makeSamlObjectBuilder(LogoutResponse.DEFAULT_ELEMENT_NAME);

    private static final SAMLObjectBuilder<StatusCode> statusCodeBuilder =
            makeSamlObjectBuilder(StatusCode.DEFAULT_ELEMENT_NAME);

    private static final SAMLObjectBuilder<Status> statusBuilder =
            makeSamlObjectBuilder(Status.DEFAULT_ELEMENT_NAME);

    private static final SAMLObjectBuilder<StatusMessage> statusMessageBuilder =
            makeSamlObjectBuilder(StatusMessage.DEFAULT_ELEMENT_NAME);

    @SuppressWarnings("unchecked")
    private static <T extends SAMLObject> SAMLObjectBuilder<T> makeSamlObjectBuilder(QName name) {
        return (SAMLObjectBuilder<T>) objectBuilderFactory.getBuilder(name);
    }

    private static final IdentifierGenerator idGenerator;

    static {
        try {
            idGenerator = new SecureRandomIdentifierGenerator();
        }
        catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }


    public SAMLMessageContext createAuthnRequest(String relayState) throws MetadataProviderException {

        SAMLMessageContext<SAMLObject, AuthnRequest, NameID> context =
                new BasicSAMLMessageContext<SAMLObject, AuthnRequest, NameID>();

        SAMLMetadata samlMetadata = Configuration.getInstance().getSAMLMetadata();
        MetadataProvider metadataProvider = samlMetadata.getMetadataProvider();

        // get local entity (SP)
        EntityDescriptor localEntity = metadataProvider.getEntityDescriptor(samlMetadata.getSpEntityId());
        SPSSODescriptor spssoDescriptor = localEntity.getSPSSODescriptor(SAML20P_NS);
        OpenSAMLUtils.initializeLocalEntity(context, localEntity, spssoDescriptor,
                AssertionConsumerService.DEFAULT_ELEMENT_NAME);

        // get peer entity (IdP)
        EntityDescriptor peerEntity = metadataProvider.getEntityDescriptor(samlMetadata.getIdpEntityId());
        IDPSSODescriptor idpssoDescriptor = peerEntity.getIDPSSODescriptor(SAML20P_NS);
        OpenSAMLUtils.initializePeerEntity(context, peerEntity, idpssoDescriptor,
                SingleSignOnService.DEFAULT_ELEMENT_NAME,
                SAML2_REDIRECT_BINDING_URI);

        // create authentication request
        AuthnRequest authnRequest = authnRequestBuilder.buildObject();
        authnRequest.setID(idGenerator.generateIdentifier());
        authnRequest.setVersion(SAMLVersion.VERSION_20);
        authnRequest.setIssueInstant(new DateTime());
        authnRequest.setProviderName(localEntity.getEntityID());
        authnRequest.setIsPassive(false);
        authnRequest.setAssertionConsumerServiceIndex(spssoDescriptor.getDefaultAssertionConsumerService().getIndex());

        Issuer issuer = issuerBuilder.buildObject();
        issuer.setValue(context.getOutboundMessageIssuer());
        authnRequest.setIssuer(issuer);

        NameIDPolicy nameIDPolicy = nameIdPolicyBuilder.buildObject();
        nameIDPolicy.setFormat(NameIDType.TRANSIENT);

        nameIDPolicy.setAllowCreate(true);
        authnRequest.setNameIDPolicy(nameIDPolicy);

        // set relay state
        context.setRelayState(relayState);

        context.setOutboundSAMLMessage(authnRequest);
        return context;
    }

    public void encodeRequest(HttpServletResponse response, SAMLMessageContext context)
            throws MessageEncodingException {
        context.setOutboundMessageTransport(new HttpServletResponseAdapter(response, true));
        MessageEncoder encoder = new HTTPRedirectDeflateEncoder();
        encoder.encode(context);
    }

    public void createLogoutResponse(Status status, SAMLMessageContext context)
            throws MetadataProviderException, SAMLException, MessageEncodingException {

        SAMLMetadata samlMetadata = Configuration.getInstance().getSAMLMetadata();
        MetadataProvider metadataProvider = samlMetadata.getMetadataProvider();

        // get local entity (SP)
        EntityDescriptor localEntity = metadataProvider.getEntityDescriptor(samlMetadata.getSpEntityId());
        SPSSODescriptor spssoDescriptor = localEntity.getSPSSODescriptor(SAML20P_NS);
        OpenSAMLUtils.initializeLocalEntity(context, localEntity, spssoDescriptor,
                AssertionConsumerService.DEFAULT_ELEMENT_NAME);

        // get peer entity (IdP)
        EntityDescriptor peerEntity = metadataProvider.getEntityDescriptor(samlMetadata.getIdpEntityId());
        IDPSSODescriptor idpssoDescriptor = peerEntity.getIDPSSODescriptor(SAML20P_NS);
        OpenSAMLUtils.initializePeerEntity(context, peerEntity, idpssoDescriptor,
                SingleSignOnService.DEFAULT_ELEMENT_NAME,
                SAML2_REDIRECT_BINDING_URI);

        String binding = OpenSAMLUtils.getLogoutBinding(idpssoDescriptor, spssoDescriptor);
        SingleLogoutService logoutService = OpenSAMLUtils.getLogoutServiceForBinding(idpssoDescriptor, binding);

        // create logout response
        LogoutResponse logoutResponse = logoutResponseBuilder.buildObject();

        logoutResponse.setID(idGenerator.generateIdentifier());
        logoutResponse.setVersion(SAMLVersion.VERSION_20);
        logoutResponse.setIssueInstant(new DateTime());
        logoutResponse.setInResponseTo(context.getOutboundSAMLMessageId());
        logoutResponse.setDestination(logoutService.getLocation());
        logoutResponse.setStatus(status);

        // set issuer
        Issuer issuer = issuerBuilder.buildObject();
        issuer.setValue(context.getOutboundMessageIssuer());
        logoutResponse.setIssuer(issuer);


        context.setCommunicationProfileId("urn:oasis:names:tc:SAML:2.0:profiles:SSO:logout");
        context.setOutboundMessage(logoutResponse);
        context.setOutboundSAMLMessage(logoutResponse);
        context.setPeerEntityEndpoint(logoutService);
        context.setPeerEntityId(idpssoDescriptor.getID());
        context.setPeerEntityRoleMetadata(idpssoDescriptor);
    }

    public Status getStatus(String code, String statusMessage) {
        StatusCode statusCode = statusCodeBuilder.buildObject();
        statusCode.setValue(code);

        Status status = statusBuilder.buildObject();
        status.setStatusCode(statusCode);

        if (statusMessage != null) {
            StatusMessage statusMessageObject = statusMessageBuilder.buildObject();
            statusMessageObject.setMessage(statusMessage);
            status.setStatusMessage(statusMessageObject);
        }

        return status;
    }
}
