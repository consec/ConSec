package org.ow2.contrail.resource.auditing;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.QueueingConsumer;
import org.ow2.contrail.resource.auditing.cadf.*;
import org.ow2.contrail.resource.auditing.cadf.ext.Initiator;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class AuditorTest {
    private static final String CONF_FILE = "src/test/resources/auditing-test.properties";

    private boolean listenerReady = false;
    private final Object lock = new Object();

    public static void main(String[] args) throws Exception {
        Conf.load(CONF_FILE);
        new AuditorTest().testAuditEvent();
    }

    private void testAuditEvent() throws Exception {
        AuditEventsListener auditEventsListener = new AuditEventsListener();
        Thread t = new Thread(auditEventsListener);
        t.start();
        synchronized (lock) {
            lock.wait(10000);
            if (!listenerReady) {
                System.out.println("Failed to start listener.");
                return;
            }
        }

        System.out.println("Starting to audit...");
        Auditor auditor = new Auditor(Conf.getRabbitMQHost(), Conf.getRabbitMQPort(), Conf.getRabbitMQExchange());

        for (int i = 0; i < 3; i++) {
            System.out.println("Auditing event " + i);
            AuditRecord auditRecord = createAuditRecord();
            auditor.audit(auditRecord);
            Thread.sleep(1000);
        }

        auditor.close();
        t.interrupt();
    }

    private AuditRecord createAuditRecord() {
        CADFEventRecord event = new CADFEventRecord();
        event.setId(UUID.randomUUID().toString());
        event.setEventType(EventType.ACTIVITY);
        event.setEventTime(new Date());
        event.setAction("create");
        event.setOutcome(Outcome.SUCCESS);

        Initiator initiator = new Initiator();
        initiator.setId("contrail-federation-cli");
        initiator.setOauthAccessToken("f74a6db9-9afb-4788-ae66-93bb768b777e");
        event.setInitiator(initiator);

        Resource target = new Resource();
        target.setId("contrail-federation-api");
        List<Attachment> attachments = new ArrayList<Attachment>();
        Attachment requestData = new Attachment();
        attachments.add(requestData);
        requestData.setContentType("application-json");
        requestData.setContent("{\"method\":\"POST\",\"uri\":\"http://contrail.xlab.si:8080/federation-api/providers/b9d3e839-347e-4382-ba30-5cda312ad55f/servers\",\"content\":{\"name\":\"server001.myprovider.com\"}}");
        target.setAttachments(attachments);
        Geolocation geolocation = new Geolocation();
        geolocation.setRegionICANN("si");
        geolocation.setCity("Ljubljana");
        target.setGeolocation(geolocation);
        event.setTarget(target);

        return event;
    }

    class AuditEventsListener implements Runnable {

        @Override
        public void run() {

            Channel channel = null;
            Connection connection = null;
            try {
                ConnectionFactory factory = new ConnectionFactory();
                factory.setHost(Conf.getRabbitMQHost());
                connection = factory.newConnection();
                channel = connection.createChannel();

                channel.exchangeDeclare(Conf.getRabbitMQExchange(), "topic", true, true, false, null);
                String queueName = channel.queueDeclare().getQueue();
                channel.queueBind(queueName, Conf.getRabbitMQExchange(), "#");
                QueueingConsumer consumer = new QueueingConsumer(channel);
                channel.basicConsume(queueName, true, consumer);
                listenerReady = true;
                System.out.println("AuditEventsListener ready.");
                synchronized (lock) {
                    lock.notify();
                }

                while (!Thread.interrupted()) {
                    QueueingConsumer.Delivery delivery = consumer.nextDelivery();

                    String message = new String(delivery.getBody());
                    String routingKey = delivery.getEnvelope().getRoutingKey();
                    System.out.println(String.format("New message arrived with routing key '%s':\n%s", routingKey, message));
                }
            }
            catch (InterruptedException e) {
                System.out.println("AuditEventsListener stopped.");
            }
            catch (Exception e) {
                e.printStackTrace();
            }
            finally {
                try {
                    channel.close();
                    connection.close();
                }
                catch (Exception ignored) {
                }
            }
        }
    }
}
