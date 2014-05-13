package org.ow2.contrail.resource.auditing;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.apache.log4j.Logger;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;
import org.codehaus.jackson.map.annotate.JsonSerialize;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.concurrent.ArrayBlockingQueue;

public class Auditor {
    private static Logger log = Logger.getLogger(Auditor.class);
    private static final String DATE_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";
    private static final String EXCHANGE_NAME = "audit_events";
    private static final int QUEUE_CAPACITY = 30;
    private static final int CONNECTION_RETRY_PERIOD = 60;

    private AuditorEngine auditorEngine;
    private Thread auditorEngineThread;
    private ConnectionFactory factory;
    private Connection connection;
    private Channel channel;
    private String exchangeName;

    public Auditor(String rabbitMQHost, int rabbitMQPort) throws IOException {
        this(rabbitMQHost, rabbitMQPort, EXCHANGE_NAME);
    }

    public Auditor(String rabbitMQHost, int rabbitMQPort, String exchangeName) throws IOException {
        this.exchangeName = exchangeName;
        factory = new ConnectionFactory();
        factory.setHost(rabbitMQHost);
        factory.setPort(rabbitMQPort);

        auditorEngine = new AuditorEngine();
        auditorEngineThread = new Thread(auditorEngine);
        auditorEngineThread.start();
        log.debug("Auditor initialized successfully.");
    }

    public void close() throws IOException {
        auditorEngineThread.interrupt();
        log.debug("Auditor was closed.");
    }

    public void audit(AuditRecord auditRecord) {
        auditorEngine.audit(auditRecord);
    }

    private class AuditorEngine implements Runnable {
        private Logger log = Logger.getLogger(AuditorEngine.class);

        private ArrayBlockingQueue<AuditRecord> workQueue;
        private ObjectMapper objectMapper;

        private AuditorEngine() {
            workQueue = new ArrayBlockingQueue<AuditRecord>(QUEUE_CAPACITY);
            objectMapper = createObjectMapper();

            log.debug("AuditorEngine initialized successfully.");
        }

        public void audit(AuditRecord auditRecord) {
            try {
                workQueue.add(auditRecord);
                log.debug("Work queue size: " + workQueue.size());
            }
            catch (IllegalStateException e) {
                log.error("Auditor queue is full. Audit event was discarded.");
            }
        }

        @Override
        public void run() {
            log.debug("AuditorEngine started.");
            try {
                connect();

                while (!Thread.currentThread().isInterrupted()) {
                    log.debug("Waiting for audit events...");
                    AuditRecord auditRecord = workQueue.take();
                    log.debug("Received audit event.");
                    if (!channel.isOpen()) {
                        log.info("Connection to RabbitMQ server failed.");
                        connect();
                    }

                    try {
                        String content = objectMapper.writeValueAsString(auditRecord);
                        channel.basicPublish(exchangeName, "", null, content.getBytes());
                        if (log.isDebugEnabled()) {
                            log.debug(String.format("Audit event %s has been published.", auditRecord.getId()));
                        }
                    }
                    catch (Exception e) {
                        log.error("Failed to publish audit event: " + e.getMessage(), e);
                    }

                }
            }
            catch (InterruptedException e) {
                log.debug("AuditorEngine was interrupted.");
                disconnect();
            }
        }

        private void connect() throws InterruptedException {
            log.debug(String.format("Trying to connect to RabbitMQ server %s:%d", factory.getHost(), factory.getPort()));
            if (connection != null && connection.isOpen()) {
                try {
                    connection.close();
                }
                catch (IOException e) {
                    // ignore
                }
            }
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    connection = factory.newConnection();
                    channel = connection.createChannel();
                    channel.exchangeDeclare(exchangeName, "topic", true, true, false, null);
                    log.info("Connection to RabbitMQ server established successfully.");
                    break;
                }
                catch (Exception e) {
                    log.error(
                            String.format("Failed to connect to the RabbitMQ server %s:%d. Retrying in %d seconds.",
                                    factory.getHost(), factory.getPort(), CONNECTION_RETRY_PERIOD));
                    Thread.sleep(CONNECTION_RETRY_PERIOD * 1000);
                }
            }
        }

        private void disconnect() {
            log.debug("Closing connection to RabbitMQ server.");
            try {
                channel.close();
            }
            catch (Exception e) {
                // ignore
            }
            try {
                connection.close();
            }
            catch (Exception e) {
                // ignore
            }
            log.debug("Connection to RabbitMQ server was closed.");
        }
    }

    static ObjectMapper createObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setSerializationInclusion(JsonSerialize.Inclusion.NON_NULL);
        objectMapper.configure(SerializationConfig.Feature.WRITE_DATES_AS_TIMESTAMPS, false);
        SimpleDateFormat sdf = new SimpleDateFormat(DATE_PATTERN);
        objectMapper.setDateFormat(sdf);
        return objectMapper;
    }
}
