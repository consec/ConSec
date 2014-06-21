package org.consec.auditing.auditserver.rabbitmq;

import org.apache.log4j.Logger;
import org.consec.auditing.auditserver.rabbitmq.utils.Conf;
import org.consec.auditing.client.Auditor;
import org.consec.auditing.client.AuditorFactory;
import org.consec.auditing.common.AuditEvent;
import org.consec.auditing.common.cadf.*;
import org.consec.auditing.common.cadf.ext.Initiator;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class AuditMessageConsumerTest {
    private static final String CONF_FILE = "src/test/resources/test.properties";
    private static final int NUMBER_OF_AUDIT_RECORDS = 3;

    private static Logger log = Logger.getLogger(AuditMessageConsumerTest.class);

    public static void main(String[] args) throws Exception {
        Conf.load(CONF_FILE);
        AuditorFactory.init(Conf.getProps());

        new AuditMessageConsumerTest().test();
    }

    public void test() throws Exception {
        AuditMessageConsumer auditMessageConsumer = new AuditMessageConsumer();
        auditMessageConsumer.start();
        Thread.sleep(500);

        Auditor auditor = AuditorFactory.getAuditor();

        for (int i = 0; i < NUMBER_OF_AUDIT_RECORDS; i++) {
            log.info("Auditing event " + i);
            AuditEvent auditEvent = createAuditEvent();
            auditor.audit(auditEvent);
            Thread.sleep(1000);
        }

        auditor.close();
        auditMessageConsumer.close();
    }

    private CADFEventRecord createAuditEvent() {
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
}
