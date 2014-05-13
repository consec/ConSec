package org.ow2.contrail.provider.storagemanager;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.QueueingConsumer;
import org.apache.log4j.Logger;
import org.ow2.contrail.provider.storagemanager.utils.MongoDBConnection;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import java.io.IOException;
import java.io.StringReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MetricsDataListener implements Runnable {
    private static Logger log = Logger.getLogger(MetricsDataListener.class);
    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
    private DBCollection rawCollection;
    private GraphiteDispatcher graphiteDispatcher;

    public MetricsDataListener() throws Exception {
        DB db = MongoDBConnection.getDB(Conf.getInstance().getMongoDatabase());
        this.rawCollection = db.getCollection(Conf.RAW_COLL_NAME);
        if (Conf.getInstance().isGraphiteDispatcherEnabled()) {
            this.graphiteDispatcher = new GraphiteDispatcher();
        }
        else {
            log.info("GraphiteDispatcher is disabled.");
        }
    }

    @Override
    public void run() {
        Channel channel = null;

        try {
            QueueingConsumer consumer = null;
            while (true) {

                try {
                    if (channel == null || !channel.isOpen()) {
                        String rabbitMQHost = Conf.getInstance().getRabbitMQHost();
                        String exchangeName = Conf.getInstance().getRabbitMQMetricsDataExchangeName();

                        log.trace("Trying to connect to RabbitMQ server " + rabbitMQHost);
                        ConnectionFactory factory = new ConnectionFactory();
                        factory.setHost(rabbitMQHost);
                        Connection connection = factory.newConnection();
                        channel = connection.createChannel();
                        channel.exchangeDeclare(exchangeName, "topic", false, true, false, null);
                        String queueName = channel.queueDeclare().getQueue();
                        channel.queueBind(queueName, exchangeName, "#");
                        consumer = new QueueingConsumer(channel);
                        channel.basicConsume(queueName, true, consumer);
                        log.info("Connection to RabbitMQ server established successfully.");
                    }
                }
                catch (IOException e) {
                    log.error(String.format("Failed to connect to RabbitMQ server: %s. Retrying in 5 minutes.",
                            e.getMessage()));
                    Thread.sleep(5 * 60 * 1000);
                    continue;
                }

                QueueingConsumer.Delivery delivery = consumer.nextDelivery();

                String routingKey = null;
                String message = null;
                DBObject record = null;
                try {
                    message = new String(delivery.getBody());
                    routingKey = delivery.getEnvelope().getRoutingKey();
                    if (log.isTraceEnabled()) {
                        log.trace(String.format("New message arrived with routing key '%s':\n%s", routingKey, message));
                    }
                    record = parseValuesMessage(message, routingKey);
                }
                catch (Exception e) {
                    log.error(String.format("Invalid message received:\nrouting key: %s\nmessage: %s",
                            routingKey, message), e);
                }

                try {
                    rawCollection.insert(record);
                    log.trace("Metrics data was stored successfully.");
                }
                catch (Exception e) {
                    log.error(String.format("Failed to store metrics data:\nrouting key: %s\nmessage: %s",
                            routingKey, message), e);
                }

                if (Conf.getInstance().isGraphiteDispatcherEnabled()) {
                    try {
                        graphiteDispatcher.sendMetricsData((BasicDBObject) record);
                        log.trace("Metrics data was successfully sent to Graphite.");
                    }
                    catch (Exception e) {
                        log.error("Failed to send metric data to Graphite: " + e.getMessage(), e);
                    }
                }
            }
        }
        catch (InterruptedException e) {
            log.trace("Interrupt received.");
        }
        finally {
            try {
                channel.close();
            }
            catch (Exception ignored) {
            }
        }
    }

    DBObject parseValuesMessage(String message, String routingKey) throws Exception {
        // parse routing key
        Pattern routingKeyPattern = Pattern.compile("^input\\.(\\w+)\\.([\\w-]+)\\.(\\w+)");
        Matcher m = routingKeyPattern.matcher(routingKey);
        if (!m.find()) {
            throw new Exception("Invalid routing key: " + routingKey);
        }
        String source = m.group(1);
        //String sid = m.group(2);
        String group = m.group(3);

        DBObject record = new BasicDBObject();
        record.put("source", source);
        record.put("group", group);

        // parse message
        Document xmlDoc;
        XPath xPath = XPathFactory.newInstance().newXPath();
        try {
            xmlDoc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(
                    new InputSource(new StringReader(message)));
        }
        catch (Exception e) {
            throw new Exception("Invalid xml document: " + e.getMessage());
        }

        XPathExpression xPathExpression = xPath.compile("/Message/@sid");
        String sid = (String) xPathExpression.evaluate(xmlDoc, XPathConstants.STRING);
        record.put("sid", sid);

        xPathExpression = xPath.compile("/Message/@time");
        String timeString = (String) xPathExpression.evaluate(xmlDoc, XPathConstants.STRING);
        Date time = sdf.parse(timeString);
        record.put("time", time);

        xPathExpression = xPath.compile("/Message/Value");
        NodeList valueNodes = (NodeList) xPathExpression.evaluate(xmlDoc, XPathConstants.NODESET);
        DBObject metrics = new BasicDBObject();
        for (int i = 0; i < valueNodes.getLength(); i++) {
            Element valueNode = (Element) valueNodes.item(i);
            String metricName = valueNode.getAttribute("id");
            String valueString = valueNode.getFirstChild().getNodeValue();
            Object value;
            if (valueString.matches("[+-]?\\d+\\.\\d+")) {
                value = Double.parseDouble(valueString);
            }
            else if (valueString.matches("[+-]?\\d+")) {
                value = Integer.parseInt(valueString);
            }
            else {
                value = valueString;
            }
            metrics.put(metricName, value);
        }
        record.put("metrics", metrics);

        return record;
    }
}
