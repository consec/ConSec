package org.consec.dynamicca.utils;

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

    public void load(File confFile) throws Exception {
        props = new Properties();
        try {
            props.load(new FileInputStream(confFile));
            log.info(String.format("Configuration loaded successfully from file '%s'.", confFile));
        }
        catch (IOException e) {
            throw new Exception(String.format("Failed to read configuration file '%s': %s", confFile, e.getMessage()));
        }
    }

    public String getRootCAPrivateKeyFile() {
        return props.getProperty("rootca.privateKey.file");
    }

    public String getRootCAPrivateKeyPass() {
        String password = props.getProperty("rootca.privateKey.password");
        return (password != null && !password.equals("")) ? password : null;
    }

    public String getRootCACertFile() {
        return props.getProperty("rootca.certificate.file");
    }

    public int getCACertLifetimeDays() {
        return Integer.parseInt(props.getProperty("ca.cert.lifetime.days"));
    }

    public String getCACertDNTemplate() {
        return props.getProperty("ca.cert.DN");
    }

    public String getUserCertDNTemplate() {
        return props.getProperty("user.cert.DN");
    }
}
