package org.consec.oauth2.authzserver.utils;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.opensaml.saml2.metadata.provider.MetadataProviderException;
import org.consec.oauth2.authzserver.saml.SAMLMetadata;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class Configuration {
    private static String propertiesFile = "src/main/conf/oauth-as.properties";
    private static Configuration instance = new Configuration();
    private static Logger log = Logger.getLogger(Configuration.class);
    private Properties props;

    private SAMLMetadata samlMetadata = null;

    public static Configuration getInstance() {
        return instance;
    }

    private Configuration() {
    }

    public void load() throws Exception {
        load(new File(propertiesFile));
    }

    public void load(File confFile) throws Exception {
        log.info(String.format("Loading configuration from file '%s'.", confFile));
        props = new Properties();
        try {
            props.load(new FileInputStream(confFile));
            log.info("Configuration loaded successfully.");
        }
        catch (IOException e) {
            throw new Exception(String.format("Failed to read configuration file '%s': %s", confFile, e.getMessage()));
        }

        samlMetadata = new SAMLMetadata(getSAMLMetadataFile(), getSamlIdpEntityId(), getSamlSpEntityId());
    }

    public int getAuthzCodeTimeout() {
        return Integer.parseInt(props.getProperty("authzCode.timeout"));
    }

    public int getAccessTokenTimeout() {
        return Integer.parseInt(props.getProperty("accessToken.timeout"));
    }

    public String getAuthenticateServiceUri() {
        return props.getProperty("login.authenticateServiceUri");
    }

    public File getSAMLMetadataFile() {
        return new File(props.getProperty("saml.metadataFile"));
    }

    public String getSamlSpEntityId() {
        return props.getProperty("saml.SP.entityID");
    }

    public String getSamlIdpEntityId() {
        return props.getProperty("saml.IdP.entityID");
    }

    public SAMLMetadata getSAMLMetadata() throws MetadataProviderException {
        return samlMetadata;
    }
    public JSONObject getSAMLAttrsMapping() throws JSONException {
        return new JSONObject(props.getProperty("saml.attributesMapping"));
    }
}
