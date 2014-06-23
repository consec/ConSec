package org.consec.auditing.auditserver.rabbitmq;

import org.apache.log4j.Logger;
import org.consec.auditing.auditserver.rabbitmq.utils.Conf;

public class Main {
    private static Logger log = Logger.getLogger(Main.class);

    public static void main(String[] args) {
        if (args.length != 2 ||
                !args[0].equals("--config")) {
            System.out.println("Usage: Main --config <file>");
            System.exit(1);
        }
        String confFile = args[1];

        try {
            Conf.load(confFile);
            AuditMessageConsumer auditMessageConsumer = new AuditMessageConsumer();
            auditMessageConsumer.start();
            System.out.println("audit-server-amqp started successfully.");
        }
        catch (Exception e) {
            log.error("audit-server-amqp failed to start: " + e.getMessage(), e);
            System.out.println("audit-server-amqp failed to start: " + e.getMessage());
        }
    }
}
