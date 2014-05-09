package org.consec.oauth2.authzserver;

import org.junit.Before;
import org.junit.Test;
import org.opensaml.Configuration;
import org.opensaml.DefaultBootstrap;
import org.opensaml.common.IdentifierGenerator;
import org.opensaml.common.SAMLObjectBuilder;
import org.opensaml.common.binding.BasicSAMLMessageContext;
import org.opensaml.common.binding.SAMLMessageContext;
import org.opensaml.common.impl.SecureRandomIdentifierGenerator;
import org.opensaml.saml1.core.Assertion;
import org.opensaml.saml2.core.Status;
import org.opensaml.saml2.core.StatusCode;
import org.opensaml.xml.ConfigurationException;
import org.opensaml.xml.XMLObjectBuilderFactory;
import org.consec.oauth2.authzserver.saml.SAMLMessageBuilder;

import java.io.File;
import java.security.NoSuchAlgorithmException;

import static org.junit.Assert.assertNotNull;

public class OpenSAMLTest {

    static {
        try {
            DefaultBootstrap.bootstrap();
        }
        catch (ConfigurationException e) {
            throw new IllegalStateException(e);
        }
    }

    private static final XMLObjectBuilderFactory objectBuilderFactory =
            Configuration.getBuilderFactory();

    private static final IdentifierGenerator idGenerator;

    static {
        try {
            idGenerator = new SecureRandomIdentifierGenerator();
        }
        catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    @Before
    public void setUp() throws Exception {
        org.consec.oauth2.authzserver.utils.Configuration.getInstance().load(
                new File("src/test/resources/oauth-as-test.properties"));
    }

    @Test
    public void createAuthnRequest() {
        // Get the builder factory
        XMLObjectBuilderFactory builderFactory = Configuration.getBuilderFactory();

        // Get the assertion builder based on the assertion element name
        @SuppressWarnings("unchecked")
        SAMLObjectBuilder<Assertion> builder = (SAMLObjectBuilder<Assertion>) builderFactory.getBuilder(Assertion.DEFAULT_ELEMENT_NAME);

        // Create the assertion
        Assertion assertion = builder.buildObject();

        assertNotNull(assertion);
    }

    @Test
    public void create() throws Exception {

        SAMLMessageBuilder builder = new SAMLMessageBuilder();
        SAMLMessageContext context = builder.createAuthnRequest(null);
        assertNotNull(context.getOutboundSAMLMessage());
    }

    @Test
    public void createLogoutResponse() throws Exception {

        SAMLMessageBuilder builder = new SAMLMessageBuilder();
        Status status = builder.getStatus(StatusCode.REQUEST_DENIED_URI, "Message signature is required.");
        SAMLMessageContext context = new BasicSAMLMessageContext();
        builder.createLogoutResponse(status, context);
        assertNotNull(context.getOutboundSAMLMessage());
        assertNotNull(context.getOutboundMessageIssuer());
    }
}
