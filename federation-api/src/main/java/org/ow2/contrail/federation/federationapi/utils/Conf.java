package org.ow2.contrail.federation.federationapi.utils;

import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Properties;

public class Conf {
    private static String propertiesFile = "src/main/config/federation-api.cfg";
    private static Conf instance = new Conf();
    private static Logger log = Logger.getLogger(Conf.class);
    private Properties props;
    private String appDataRoot;

    public static final String FEDERATION_PROVIDER_UUID = "dadb2c20-5351-11e3-8f96-0800200c9a66";

    public static Conf getInstance() {
        return instance;
    }

    private Conf() {
    }

    public void load() throws Exception {
        load(new File(propertiesFile));
    }

    public void load(File confFile) throws Exception {
        log.info(String.format("Loading configuration from file '%s'.", confFile));
        props = new Properties();
        try {
            props.load(new FileInputStream(confFile));
            log.info("Conf loaded successfully.");
        }
        catch (IOException e) {
            throw new Exception(String.format("Failed to read configuration file '%s': %s", confFile, e.getMessage()));
        }
    }

    public String getSlaNegotiationEndpoint() {
        return props.getProperty("sla.negotiation.endpoint");
    }

    public NegotiationEngine getSlaNegotiationEngine() {
        return NegotiationEngine.valueOf(props.getProperty("sla.negotiation.engine"));
    }

    public String getAppDataRoot() {
        return appDataRoot;
    }

    public void setAppDataRoot(String appDataRoot) {
        this.appDataRoot = appDataRoot;
    }

    public String getOAuthClientId() {
        return props.getProperty("oauthClient.id");
    }

    public String getOAuthClientSecret() {
        return props.getProperty("oauthClient.secret");
    }

    public String getOAuthClientKeystoreFile() {
        return props.getProperty("oauthClient.keystore.file");
    }

    public String getOAuthClientKeystorePass() {
        return props.getProperty("oauthClient.keystore.pass");
    }

    public String getOAuthClientTruststoreFile() {
        return props.getProperty("oauthClient.truststore.file");
    }

    public String getOAuthClientTruststorePass() {
        return props.getProperty("oauthClient.truststore.pass");
    }

    public String getOAuthASTokenEndpoint() {
        return props.getProperty("oauthClient.tokenEndpoint");
    }

    public URI getAddressOAuthAS() throws URISyntaxException {
        String address = props.getProperty("address.oauth-as");
        if (!address.endsWith("/")) {
            address += "/";
        }
        return new URI(address);
    }

    public URI getAddressAuditManager() throws URISyntaxException {
        String address = props.getProperty("address.audit-manager");
        if (!address.endsWith("/")) {
            address += "/";
        }
        return new URI(address);
    }

    public static enum NegotiationEngine {
        MOCK,
        SLASOI
    }
}
