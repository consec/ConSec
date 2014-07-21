package org.consec.auditing.client.amqp;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.QueueingConsumer;
import org.consec.auditing.client.amqp.utils.Conf;
import org.consec.auditing.common.auditevent.AuditEvent;
import org.consec.auditing.common.auditevent.Severity;
import org.consec.auditing.common.auditevent.Target;

import java.util.Date;

public class AmqpAuditorTest {
    private static final String CONF_FILE = "src/test/resources/test.properties";

    private boolean listenerReady = false;
    private final Object lock = new Object();

    public static void main(String[] args) throws Exception {
        Conf.load(CONF_FILE);
        new AmqpAuditorTest().testAuditor();
    }

    private void testAuditor() throws Exception {
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
        AmqpAuditor amqpAuditor = new AmqpAuditor(Conf.getProps());

        for (int i = 0; i < 3; i++) {
            System.out.println("Auditing event " + i);
            AuditEvent auditEvent = createAuditEvent();
            amqpAuditor.audit(auditEvent);
            Thread.sleep(1000);
        }

        amqpAuditor.close();
        t.interrupt();
    }

    private AuditEvent createAuditEvent() {
        AuditEvent auditEvent = new AuditEvent();
        auditEvent.setAction("READ");
        auditEvent.setEventTime(new Date());
        auditEvent.setEventType("REST_API_CALL");
        org.consec.auditing.common.auditevent.Initiator initiator = new org.consec.auditing.common.auditevent.Initiator("test_user");
        initiator.setType("USER");
        auditEvent.setInitiator(initiator);
        Target target = new Target("consec-server1/federation-api");
        target.setType("WEB_SERVICE");
        auditEvent.setSeverity(Severity.INFO);
        auditEvent.setOutcome(org.consec.auditing.common.auditevent.Outcome.SUCCESS);

        return auditEvent;
    }

    class AuditEventsListener implements Runnable {

        @Override
        public void run() {

            Channel channel = null;
            Connection connection = null;
            try {
                ConnectionFactory factory = new ConnectionFactory();
                factory.setHost(Conf.getAmqpServerHost());
                connection = factory.newConnection();
                channel = connection.createChannel();

                channel.exchangeDeclare(Conf.getExchangeName(), "topic", true, true, false, null);
                String queueName = channel.queueDeclare().getQueue();
                channel.queueBind(queueName, Conf.getExchangeName(), "#");
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
