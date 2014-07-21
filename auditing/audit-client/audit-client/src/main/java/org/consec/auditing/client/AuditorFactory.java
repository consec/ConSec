package org.consec.auditing.client;

import org.apache.log4j.Logger;

import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.Properties;

public class AuditorFactory {
    private static Logger log = Logger.getLogger(AuditorFactory.class);
    private static Auditor auditor;

    private AuditorFactory() {
    }

    public static void init(String confFilePath) throws Exception {
        Properties props = new Properties();
        try {
            props.load(new FileInputStream(confFilePath));
            log.info(String.format("Configuration loaded successfully from file '%s'.", confFilePath));
        }
        catch (IOException e) {
            throw new Exception(String.format("Failed to read configuration file '%s': %s", confFilePath, e.getMessage()));
        }

        init(props);
    }

    public static void init(Properties props) throws Exception {
        if (auditor != null) {
            throw new Exception("AuditorFactory already initialized.");
        }

        // create auditor instance
        String auditorClass = props.getProperty(Conf.AUDITOR_IMPL_PROP);
        Class<?> clazz = Class.forName(auditorClass);
        Constructor<?> constructor = clazz.getConstructor(Properties.class);
        auditor = (Auditor) constructor.newInstance(props);
    }

    public static Auditor getAuditor() {
        if (auditor == null) {
            throw new RuntimeException("AuditorFactory is not initialized.");
        }

        return auditor;
    }
}
