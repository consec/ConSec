package org.ow2.contrail.provider.storagemanager;

import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.util.JSON;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.QueueingConsumer;
import org.apache.log4j.Logger;
import org.codehaus.jackson.map.ObjectMapper;
import org.ow2.contrail.provider.storagemanager.utils.MongoDBConnection;
import org.ow2.contrail.resource.auditing.cadf.CADFEventRecord;

import java.text.SimpleDateFormat;

public class AuditEventsListener implements Runnable {
    private static Logger log = Logger.getLogger(AuditEventsListener.class);
    private static final String DATE_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";
    private DBCollection auditLogCollection;
    private ObjectMapper objectMapper;
    private ConnectionFactory factory;
    private Thread thread;

    public AuditEventsListener() throws Exception {
        MongoClient mongoClient = MongoDBConnection.getMongoClient();
        DB db = mongoClient.getDB(Conf.getInstance().getMongoDatabase());
        this.auditLogCollection = db.getCollection(Conf.AUDIT_LOG_COLL_NAME);

        objectMapper = new ObjectMapper();
        SimpleDateFormat sdf = new SimpleDateFormat(DATE_PATTERN);
        objectMapper.setDateFormat(sdf);

        String rabbitMQHost = Conf.getInstance().getRabbitMQHost();
        int rabbitMQPort = Conf.getInstance().getRabbitMQPort();
        factory = new ConnectionFactory();
        factory.setHost(rabbitMQHost);
        factory.setPort(rabbitMQPort);
    }

    public void start() {
        log.trace("AuditEventsListener is starting.");
        thread = new Thread(this);
        thread.start();
        log.info("AuditEventsListener has started successfully.");
    }

    public void close() {
        log.trace("AuditEventsListener is stopping.");
        thread.interrupt();
        log.info("AuditEventsListener has stopped.");
    }

    @Override
    public void run() {
        Channel channel = null;
        Connection connection = null;
        String exchangeName = Conf.getInstance().getRabbitMQAuditEventsExchangeName();

        try {
            QueueingConsumer consumer = null;
            while (!Thread.interrupted()) {

                try {
                    if (channel == null || !channel.isOpen()) {

                        if (connection != null && connection.isOpen()) {
                            log.trace("Closing connection to RabbitMQ server.");
                            connection.close();
                        }
                        log.trace(String.format("Trying to connect to RabbitMQ server %s:%d",
                                factory.getHost(), factory.getPort()));
                        connection = factory.newConnection();

                        channel = connection.createChannel();
                        channel.exchangeDeclare(exchangeName, "topic", true, true, false, null);
                        String queueName = channel.queueDeclare().getQueue();
                        channel.queueBind(queueName, exchangeName, "#");
                        consumer = new QueueingConsumer(channel);
                        channel.basicConsume(queueName, true, consumer);

                        log.info("Connection to RabbitMQ server established successfully.");
                        log.info("Waiting for messages...");
                    }
                }
                catch (Exception e) {
                    int retryTime = Conf.getInstance().getRabbitMQConnRetryTime();
                    log.error(String.format("Failed to connect to RabbitMQ server: %s. Retrying in %d seconds.",
                            e.getMessage(), retryTime));
                    Thread.sleep(retryTime * 1000);
                    continue;
                }

                QueueingConsumer.Delivery delivery = consumer.nextDelivery();

                String routingKey = null;
                String message = null;
                DBObject dbObject = null;
                try {
                    message = new String(delivery.getBody());
                    routingKey = delivery.getEnvelope().getRoutingKey();
                    if (log.isTraceEnabled()) {
                        log.trace(String.format("New message arrived with routing key '%s':\n%s", routingKey, message));
                    }

                    dbObject = convertToDBObject(message);
                }
                catch (Exception e) {
                    log.error(String.format("Invalid message received: %s\nRouting key: %s\nMessage: %s",
                            e.getMessage(), routingKey, message), e);
                    continue;
                }

                try {
                    auditLogCollection.insert(dbObject);
                    log.trace("Audit event was stored to MongoDB successfully.");
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

    private DBObject convertToDBObject(String content) throws Exception {
        // TODO: convert AuditRecord directly to Mongo DBObject?
        DBObject dbObject = (DBObject) JSON.parse(content);
        String typeURI = (String) dbObject.get("typeURI");
        log.trace("TypeURI: " + typeURI);
        if (typeURI == null) {
            throw new Exception("Invalid audit event: missing field typeURI.");
        }
        else if (typeURI.equals("http://schemas.dmtf.org/cloud/audit/1.0/event")) {
            log.trace("Audit event is of type CADFEventRecord.");
            CADFEventRecord eventRecord = objectMapper.readValue(content, CADFEventRecord.class);
            // convert eventTime field to Date
            dbObject.put("eventTime", eventRecord.getEventTime());
            return dbObject;
        }
        else {
            throw new Exception(String.format("Invalid audit event: unsupported typeURI '%s'.", typeURI));
        }
    }
}
