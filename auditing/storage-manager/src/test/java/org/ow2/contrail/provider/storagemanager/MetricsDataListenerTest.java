package org.ow2.contrail.provider.storagemanager;

import com.mongodb.DBObject;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.QueueingConsumer;
import org.junit.Ignore;
import org.junit.Test;

import java.io.FileReader;
import java.io.IOException;
import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@Ignore
public class MetricsDataListenerTest {

    private static final String EXCHANGE_NAME = "input";
    private static final String host = "10.31.1.10";

    @Test
    public void testListener() throws Exception {

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(host);
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        channel.exchangeDeclare(EXCHANGE_NAME, "topic", false, true, false, null);
        String queueName = channel.queueDeclare().getQueue();
        channel.queueBind(queueName, EXCHANGE_NAME, "#");

        System.out.println(" [*] Waiting for messages. To exit press CTRL+C");

        QueueingConsumer consumer = new QueueingConsumer(channel);
        channel.basicConsume(queueName, true, consumer);

        while (true) {
            QueueingConsumer.Delivery delivery = consumer.nextDelivery();
            String message = new String(delivery.getBody());

            System.out.println(delivery.getEnvelope().getRoutingKey());
            System.out.println(message);
            System.out.println();
        }
    }

    @Test
    public void testStoreMonitoringData() throws Exception {
        String message = readEntireFile("src/test/resources/MetricsDataMessage.xml");
        String routingKey = "input.host.n0014-xc1-xlab-lan.cpu";

        MetricsDataListener listener = new MetricsDataListener();
        DBObject record = listener.parseValuesMessage(message, routingKey);
        assertEquals(record.get("source"), "host");
        assertEquals(record.get("group"), "cpu");
        assertEquals(record.get("sid"), "n0014-xc1-xlab-lan");
        assertTrue(record.get("time") instanceof Date);

        DBObject metrics = (DBObject) record.get("metrics");
        assertEquals(metrics.get("cores"), 2);
        assertTrue(metrics.get("cores") instanceof Integer);

        assertEquals(metrics.get("speed"), 3058.887);
        assertTrue(metrics.get("speed") instanceof Double);
    }

    private static String readEntireFile(String filename) throws IOException {
        FileReader in = new FileReader(filename);
        StringBuilder contents = new StringBuilder();
        char[] buffer = new char[1024];
        int read = 0;
        do {
            contents.append(buffer, 0, read);
            read = in.read(buffer);
        } while (read >= 0);
        return contents.toString();
    }
}