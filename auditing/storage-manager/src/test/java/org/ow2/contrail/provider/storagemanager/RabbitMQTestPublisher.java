package org.ow2.contrail.provider.storagemanager;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

public class RabbitMQTestPublisher {

    public static void main(String[] args) throws Exception {
        if (args.length != 4) {
            System.out.println("Usage:\n  RabbitMQTestPublisher <rabbitmq-host> <rabbitmq-port> <exchange> <content>");
            System.exit(0);
        }
        String rabbitMqHost = args[0];
        int rabbitMqPort = Integer.parseInt(args[1]);
        String exchangeName = args[2];
        String content = args[3];

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(rabbitMqHost);
        factory.setPort(rabbitMqPort);
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();
        channel.exchangeDeclare(exchangeName, "topic", true, true, false, null);
        System.out.println("Connection to RabbitMQ server established successfully.");

        channel.basicPublish(exchangeName, "", null, content.getBytes());
        System.out.println("Message has been published successfully.");

        channel.close();
        connection.close();
    }
}
