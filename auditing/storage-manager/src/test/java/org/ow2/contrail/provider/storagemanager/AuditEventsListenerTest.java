package org.ow2.contrail.provider.storagemanager;

import org.apache.log4j.Logger;
import org.ow2.contrail.provider.storagemanager.utils.MongoDBConnection;
import org.ow2.contrail.resource.auditing.AuditRecord;
import org.ow2.contrail.resource.auditing.Auditor;
import org.ow2.contrail.resource.auditing.cadf.*;
import org.ow2.contrail.resource.auditing.cadf.ext.Initiator;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class AuditEventsListenerTest {
    private static final String CONF_FILE = "src/test/resources/storage-manager.cfg";
    private static final int NUMBER_OF_AUDIT_RECORDS = 3;

    private static Logger log = Logger.getLogger(AuditEventsListenerTest.class);

    public static void main(String[] args) throws Exception {
        new AuditEventsListenerTest().test();
    }

    public void test() throws Exception {
        Conf.getInstance().load(CONF_FILE);
        MongoDBConnection.init();
        AuditEventsListener auditEventsListener = new AuditEventsListener();
        auditEventsListener.start();
        Thread.sleep(500);

        Auditor auditor = new Auditor(
                Conf.getInstance().getRabbitMQHost(),
                Conf.getInstance().getRabbitMQPort());

        for (int i = 0; i < NUMBER_OF_AUDIT_RECORDS; i++) {
            log.info("Auditing event " + i);
            AuditRecord auditRecord = createAuditRecord();
            auditor.audit(auditRecord);
            Thread.sleep(1000);
        }

        auditor.close();
        auditEventsListener.close();
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
}
