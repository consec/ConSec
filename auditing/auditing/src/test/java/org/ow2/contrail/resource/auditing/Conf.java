package org.ow2.contrail.resource.auditing;

import org.apache.log4j.Logger;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class Conf {
    private static Logger log = Logger.getLogger(Conf.class);
    private static Properties props;

    private Conf() {
    }

    public static void load(String confFile) throws Exception {
        props = new Properties();
        try {
            props.load(new FileInputStream(confFile));
            log.info("Configuration loaded successfully from file " + confFile);
        }
        catch (IOException e) {
            throw new Exception(String.format("Failed to load configuration file '%s': %s", confFile, e.getMessage()));
        }
    }

    public static String getRabbitMQHost() {
        return props.getProperty("rabbitmq.host");
    }

    public static int getRabbitMQPort() {
        return Integer.parseInt(props.getProperty("rabbitmq.port"));
    }

    public static String getRabbitMQExchange() {
        return props.getProperty("rabbitmq.exchange");
    }
}
