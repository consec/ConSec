package org.ow2.contrail.federation.auditmanager.utils;

import org.apache.log4j.Logger;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Properties;

public class Conf {

    public static final String MONGO_JOBS_COLLECTION = "jobs";
    public static final int JOB_TIMEOUT = 300;

    private static Conf instance = new Conf();
    private static Logger log = Logger.getLogger(Conf.class);
    private Properties props;

    public static Conf getInstance() {
        return instance;
    }

    private Conf() {
    }

    public void load(String fileName) throws IOException {
        log.info(String.format("Loading audit-manager configuration from file '%s'.", fileName));
        props = new Properties();
        try {
            props.load(new FileInputStream(fileName));
            log.info("Configuration loaded successfully.");
        }
        catch (IOException e) {
            throw new IOException(String.format("Failed to read configuration file '%s'.", fileName));
        }
    }

    public String getMongoDBConnectionString() {
        return props.getProperty("mongodb.connectionString");
    }

    public String getMongoDBDatabase() {
        return props.getProperty("mongodb.database");
    }

    public int getSchedulerPoolSize() {
        return Integer.parseInt(props.getProperty("scheduler.pool.size"));
    }

    public String getKeystoreFile() {
        return props.getProperty("keystore.file");
    }

    public String getKeystorePass() {
        return props.getProperty("keystore.pass");
    }

    public String getTruststoreFile() {
        return props.getProperty("truststore.file");
    }

    public String getTruststorePass() {
        return props.getProperty("truststore.pass");
    }

    public String getOAuthClientId() {
        return props.getProperty("oauthClient.clientId");
    }

    public String getOAuthClientSecret() {
        return props.getProperty("oauthClient.clientSecret");
    }

    public URI getAddressFederationApi() throws URISyntaxException {
        String address = props.getProperty("address.federation-api");
        if (!address.endsWith("/")) {
            address += "/";
        }
        return new URI(address);
    }

    public URI getAddressOAuthAS() throws URISyntaxException {
        String address = props.getProperty("address.oauth-as");
        if (!address.endsWith("/")) {
            address += "/";
        }
        return new URI(address);
    }
}
