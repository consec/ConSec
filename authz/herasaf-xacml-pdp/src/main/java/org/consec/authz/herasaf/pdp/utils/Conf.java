package org.consec.authz.herasaf.pdp.utils;

import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class Conf {
    private static Conf instance = new Conf();
    private static Logger log = Logger.getLogger(Conf.class);
    private static Properties props;

    private Conf() {
    }

    public static void load(File confFile) throws Exception {
        props = new Properties();
        try {
            props.load(new FileInputStream(confFile));
            log.info(String.format("Configuration loaded successfully from file '%s'.", confFile));
        }
        catch (IOException e) {
            throw new Exception(String.format("Failed to read configuration file '%s': %s", confFile, e.getMessage()));
        }
    }

    public static String getPolicyRepositoryLocation() {
        return props.getProperty("policyRepository.location");
    }
}
