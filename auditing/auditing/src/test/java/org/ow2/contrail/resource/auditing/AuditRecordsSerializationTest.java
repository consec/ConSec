package org.ow2.contrail.resource.auditing;

import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Test;
import org.ow2.contrail.resource.auditing.cadf.*;
import org.ow2.contrail.resource.auditing.cadf.ext.Initiator;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;

public class AuditRecordsSerializationTest {

    @Test
    public void testSerialization() throws Exception {
        ObjectMapper objectMapper = Auditor.createObjectMapper();
        CADFEventRecord auditRecord = createAuditRecord();
        String json = objectMapper.writeValueAsString(auditRecord);
        System.out.println(json);
        CADFEventRecord auditRecord1 = objectMapper.readValue(json, CADFEventRecord.class);

        assertEquals(auditRecord.getId(), auditRecord1.getId());

        // initiator
        assertEquals(auditRecord.getInitiator().getId(), auditRecord1.getInitiator().getId());
        assertEquals(auditRecord1.getInitiator().getClass(), Initiator.class);
        assertEquals(auditRecord1.getInitiator().getTypeURI(), Initiator.TYPE_URI);
        assertEquals(((Initiator) auditRecord.getInitiator()).getOauthAccessToken(),
                ((Initiator) auditRecord1.getInitiator()).getOauthAccessToken());

        // target
        assertEquals(auditRecord.getTarget().getId(), auditRecord1.getTarget().getId());
        assertEquals(auditRecord1.getTarget().getClass(), Resource.class);
        assertEquals(auditRecord1.getTarget().getTypeURI(), Resource.TYPE_URI);
    }

    private CADFEventRecord createAuditRecord() {
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
