package org.ow2.contrail.resource.auditingapi.utils;

import org.apache.log4j.Logger;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class Conf {

    public static final String AUDIT_LOG_COLL_NAME = "auditLog";

    private static Conf instance = new Conf();
    private static Logger log = Logger.getLogger(Conf.class);
    private Properties props;

    public static Conf getInstance() {
        return instance;
    }

    private Conf() {
    }

    public void load(String fileName) throws IOException {
        log.info(String.format("Loading auditing-api configuration from file '%s'.", fileName));
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
}
