package org.ow2.contrail.provider.storagemanager;

import org.apache.log4j.Logger;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class Conf {

    public static final String RAW_COLL_NAME = "rawData";
    public static final String COMPRESSED_COLL_NAME = "compressedData";
    public static final String EVENTS_COLL_NAME = "events";
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
        log.info(String.format("Loading storage-manager configuration from file '%s'.", fileName));
        props = new Properties();
        try {
            props.load(new FileInputStream(fileName));
            log.info("Configuration loaded successfully.");
        }
        catch (IOException e) {
            throw new IOException(String.format("Failed to read configuration file '%s'.", fileName));
        }
    }

    public String getMongoConnectionString() {
        return props.getProperty("mongodb.connectionString");
    }

    public String getMongoDatabase() {
        return props.getProperty("mongodb.database");
    }

    public String getRabbitMQHost() {
        return props.getProperty("rabbitmq.host");
    }

    public int getRabbitMQPort() {
        return Integer.parseInt(props.getProperty("rabbitmq.port"));
    }

    public int getRabbitMQConnRetryTime() {
        return Integer.parseInt(props.getProperty("rabbitmq.connection.retryTime"));
    }

    public String getRabbitMQMetricsDataExchangeName() {
        return props.getProperty("rabbitmq.metricsData.exchangeName");
    }

    public String getRabbitMQAuditEventsExchangeName() {
        return props.getProperty("rabbitmq.auditEvents.exchangeName");
    }

    public String getArchiverSchedule() {
        return props.getProperty("archiver.schedule");
    }

    public boolean getDeleteArchivedData() {
        return Boolean.parseBoolean(props.getProperty("archiver.deleteArchivedData"));
    }

    public boolean isGraphiteDispatcherEnabled() {
        return Boolean.parseBoolean(props.getProperty("graphite.dispatcher.enabled"));
    }

    public String getCarbonHost() {
        return props.getProperty("graphite.carbon.host");
    }

    public int getCarbonPort() {
        return Integer.parseInt(props.getProperty("graphite.carbon.port"));
    }

    public int getHRMinIntervalLength() {
        return Integer.parseInt(props.getProperty("historyRetriever.condensedHistory.minIntervalLength"));
    }
}
