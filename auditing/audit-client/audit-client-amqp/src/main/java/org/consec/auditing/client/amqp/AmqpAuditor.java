package org.consec.auditing.client.amqp;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.apache.log4j.Logger;
import org.consec.auditing.client.Auditor;
import org.consec.auditing.client.amqp.utils.Conf;
import org.consec.auditing.common.AuditEvent;
import org.consec.auditing.common.utils.AuditEventSerializer;

import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.ArrayBlockingQueue;

public class AmqpAuditor implements Auditor {
    private static Logger log = Logger.getLogger(AmqpAuditor.class);
    private static final int QUEUE_CAPACITY = 30;
    private static final int CONNECTION_RETRY_PERIOD = 60;

    private AuditorEngine auditorEngine;
    private Thread auditorEngineThread;
    private ConnectionFactory factory;
    private Connection connection;
    private Channel channel;
    private String exchangeName;

    public AmqpAuditor(Properties props) throws IOException {
        Conf.load(props);

        this.exchangeName = Conf.getExchangeName();
        factory = new ConnectionFactory();
        factory.setHost(Conf.getAmqpServerHost());
        factory.setPort(Conf.getAmqpServerPort());

        auditorEngine = new AuditorEngine();
        auditorEngineThread = new Thread(auditorEngine);
        auditorEngineThread.start();
        log.debug("AmqpAuditor initialized successfully.");
    }

    public void close() {
        auditorEngineThread.interrupt();
        log.debug("AmqpAuditor was closed.");
    }

    public void audit(AuditEvent auditEvent) {
        auditorEngine.audit(auditEvent);
    }

    private class AuditorEngine implements Runnable {
        private Logger log = Logger.getLogger(AuditorEngine.class);

        private ArrayBlockingQueue<AuditEvent> workQueue;
        AuditEventSerializer auditEventSerializer;

        private AuditorEngine() {
            workQueue = new ArrayBlockingQueue<AuditEvent>(QUEUE_CAPACITY);
            auditEventSerializer = new AuditEventSerializer();
            log.debug("AuditorEngine initialized successfully.");
        }

        public void audit(AuditEvent auditEvent) {
            try {
                workQueue.add(auditEvent);
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
                    AuditEvent auditEvent = workQueue.take();
                    log.debug("Received audit event.");
                    if (!channel.isOpen()) {
                        log.info("Connection to AMQP server failed.");
                        connect();
                    }

                    try {
                        String content = auditEventSerializer.serialize(auditEvent);
                        channel.basicPublish(exchangeName, "", null, content.getBytes());
                        if (log.isDebugEnabled()) {
                            log.debug(String.format("Audit event %s has been published.", auditEvent.getId()));
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
            log.debug(String.format("Trying to connect to AMQP server %s:%d", factory.getHost(), factory.getPort()));
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
                    log.info("Connection to AMQP server established successfully.");
                    break;
                }
                catch (Exception e) {
                    log.error(
                            String.format("Failed to connect to the AMQP server %s:%d. Retrying in %d seconds.",
                                    factory.getHost(), factory.getPort(), CONNECTION_RETRY_PERIOD));
                    Thread.sleep(CONNECTION_RETRY_PERIOD * 1000);
                }
            }
        }

        private void disconnect() {
            log.debug("Closing connection to AMQP server.");
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
            log.debug("Connection to AMQP server was closed.");
        }
    }
}
