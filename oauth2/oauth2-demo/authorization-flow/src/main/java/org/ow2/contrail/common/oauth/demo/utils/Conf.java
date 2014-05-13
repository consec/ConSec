package org.ow2.contrail.common.oauth.demo.utils;

import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class Conf {
    private static Conf instance = new Conf();
    private static Logger log = Logger.getLogger(Conf.class);
    private Properties props;

    public static Conf getInstance() {
        return instance;
    }

    private Conf() {
    }

    public void load(File confFile) throws IOException {
        log.info(String.format("Loading configuration from file '%s'.", confFile.getAbsolutePath()));
        this.props = new Properties();
        try {
            this.props.load(new FileInputStream(confFile));
            log.info("Configuration loaded successfully.");
        }
        catch (IOException e) {
            throw new IOException(String.format("Failed to read configuration file '%s'.", confFile.getAbsolutePath()));
        }
    }

    public String getClientId() {
        return props.getProperty("client.id");
    }

    public String getClientSecret() {
        return props.getProperty("client.secret");
    }

    public String getClientOauth2CallbackUri() {
        return props.getProperty("client.oauth2callbackUri");
    }

    public String getClientKeystoreFile() {
        return props.getProperty("client.keystore.file");
    }

    public String getClientKeystorePass() {
        return props.getProperty("client.keystore.pass");
    }

    public String getClientTruststoreFile() {
        return props.getProperty("client.truststore.file");
    }

    public String getClientTruststorePass() {
        return props.getProperty("client.truststore.pass");
    }

    public String getASAuthorizationUri() {
        return props.getProperty("authzserver.authorizationEndpointUri");
    }

    public String getASAccessTokenUri() {
        return props.getProperty("authzserver.accessTokenEndpointUri");
    }

    public String getASAccessTokenValidationUri() {
        return props.getProperty("authzserver.accessTokenValidationEndpointUri");
    }

    public String getCAUserCertUri() {
        return props.getProperty("caserver.userCertUri");
    }

    public String getScope() {
        return props.getProperty("scope");
    }
}
