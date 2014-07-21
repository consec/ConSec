package org.consec.auditing.auditserver.rabbitmq;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.QueueingConsumer;
import org.apache.log4j.Logger;
import org.consec.auditing.auditserver.rabbitmq.utils.Conf;
import org.consec.auditing.common.auditevent.AuditEvent;
import org.consec.auditing.common.utils.AuditEventDeserializer;

public class AuditMessageConsumer implements Runnable {
    private static Logger log = Logger.getLogger(AuditMessageConsumer.class);
    private ConnectionFactory factory;
    private Thread thread;
    private AuditEventDeserializer auditEventDeserializer;

    public AuditMessageConsumer() throws Exception {
        factory = new ConnectionFactory();
        factory.setHost(Conf.getAmqpServerHost());
        factory.setPort(Conf.getAmqpServerPort());

        auditEventDeserializer = new AuditEventDeserializer();
    }

    public void start() {
        log.trace("AuditMessageConsumer is starting...");
        thread = new Thread(this);
        thread.start();
        log.info("AuditMessageConsumer has started successfully.");
    }

    public void close() {
        log.trace("AuditMessageConsumer is stopping...");
        thread.interrupt();
        log.info("AuditMessageConsumer has stopped.");
    }

    @Override
    public void run() {
        Channel channel = null;
        Connection connection = null;
        String exchangeName = Conf.getExchangeName();

        try {
            QueueingConsumer consumer = null;
            while (!Thread.interrupted()) {

                try {
                    if (channel == null || !channel.isOpen()) {

                        if (connection != null && connection.isOpen()) {
                            log.trace("Closing connection to AMQP server.");
                            connection.close();
                        }
                        log.trace(String.format("Trying to connect to AMQP server %s:%d",
                                factory.getHost(), factory.getPort()));
                        connection = factory.newConnection();

                        channel = connection.createChannel();
                        channel.exchangeDeclare(exchangeName, "topic", true, true, false, null);
                        String queueName = channel.queueDeclare().getQueue();
                        channel.queueBind(queueName, exchangeName, "#");
                        consumer = new QueueingConsumer(channel);
                        channel.basicConsume(queueName, true, consumer);

                        log.info("Connection to AMQP server established successfully.");
                        log.info("Waiting for messages...");
                    }
                }
                catch (Exception e) {
                    int retryTime = Conf.getConnRetryTime();
                    log.error(String.format("Failed to connect to AMQP server: %s. Retrying in %d seconds.",
                            e.getMessage(), retryTime));
                    Thread.sleep(retryTime * 1000);
                    continue;
                }

                QueueingConsumer.Delivery delivery = consumer.nextDelivery();

                String routingKey = null;
                String message = null;
                AuditEvent auditEvent;
                try {
                    message = new String(delivery.getBody());
                    routingKey = delivery.getEnvelope().getRoutingKey();
                    if (log.isTraceEnabled()) {
                        log.trace(String.format("New message arrived with routing key '%s':\n%s", routingKey, message));
                    }

                    AuditEvent auditMessage = auditEventDeserializer.deserialize(message);
                }
                catch (Exception e) {
                    log.error(String.format("Invalid message received: %s\nRouting key: %s\nMessage: %s",
                            e.getMessage(), routingKey, message), e);
                    continue;
                }

                try {
                    // TODO: store audit event
                    log.trace("Audit event was stored successfully.");
                }
                catch (Exception e) {
                    log.error("Failed to store audit event: " + e.getMessage(), e);
                }
            }
        }
        catch (InterruptedException e) {
            log.trace("Interrupt received. AuditEventsListener is exiting.");
        }
        catch (Exception e) {
            log.error("AuditEventsListener failed: " + e.getMessage(), e);
        }
        finally {
            try {
                channel.close();
            }
            catch (Exception ignored) {
            }
            try {
                connection.close();
            }
            catch (Exception ignored) {
            }
        }
    }
}
