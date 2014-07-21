package org.consec.auditing.common;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.consec.auditing.common.auditevent.*;
import org.consec.auditing.common.utils.AuditEventDeserializer;
import org.consec.auditing.common.utils.AuditEventSerializer;
import org.junit.Test;

import java.util.Date;

import static org.junit.Assert.assertEquals;

public class AuditEventSerializationTest {
    private static final String DATE_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";

    private static Logger log = Logger.getLogger(AuditEventSerializationTest.class);

    @Test
    public void testSerialization() throws Exception {
        AuditEvent auditEvent = new AuditEvent();
        auditEvent.setAction("READ");
        auditEvent.setEventTime(new Date());
        auditEvent.setEventType("REST_API_CALL");
        Initiator initiator = new Initiator("test_user");
        initiator.setType("USER");
        auditEvent.setInitiator(initiator);
        Target target = new Target("consec-server1/federation-api");
        target.setType("WEB_SERVICE");
        auditEvent.setSeverity(Severity.INFO);
        auditEvent.setOutcome(Outcome.SUCCESS);

        JSONObject httpRequestJson = new JSONObject();
        httpRequestJson.put("method", "GET");
        httpRequestJson.put("uri", "https://consec-server1/federation-api/users/523aebaa-cf04-4bd4-b067-dcf17e74ff50");
        Attachment httpRequestAttach = new Attachment("httpRequestData", "application/json", httpRequestJson.toString());
        auditEvent.addAttachment(httpRequestAttach);

        JSONObject httpResponseJson = new JSONObject();
        httpResponseJson.put("statusCode", 200);
        httpResponseJson.put("contentType", "application/json");
        httpResponseJson.put("content", "{'userId':'523aebaa-cf04-4bd4-b067-dcf17e74ff50', 'username':'test_user'}");
        Attachment httpResponseAttach = new Attachment("httpResponseData", "application/json",
                httpResponseJson.toString());
        auditEvent.addAttachment(httpResponseAttach);

        AuditEventSerializer serializer = new AuditEventSerializer();
        String data = serializer.serialize(auditEvent);
        log.debug("Serialized audit event:\n" + data);

        AuditEventDeserializer deserializer = new AuditEventDeserializer();
        AuditEvent auditEvent1 = deserializer.deserialize(data);

        assertEquals(auditEvent.getInitiator().getId(), auditEvent1.getInitiator().getId());
        assertEquals(auditEvent.getAttachments().size(), auditEvent1.getAttachments().size());

        Attachment httpResponseAttach1 = auditEvent1.getAttachments().get("httpResponseData");
        assertEquals(httpResponseAttach1.getName(), httpResponseAttach.getName());
        assertEquals(httpResponseAttach1.getContentType(), httpResponseAttach.getContentType());
        assertEquals(httpResponseAttach1.getContent(), httpResponseJson.toString());
    }
}