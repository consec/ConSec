package org.consec.auditing.common.utils;

import org.codehaus.jackson.map.ObjectMapper;
import org.consec.auditing.common.auditevent.AuditEvent;

import java.io.IOException;
import java.text.SimpleDateFormat;

public class AuditEventDeserializer {
    private static final String DATE_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";

    private ObjectMapper objectMapper;

    public AuditEventDeserializer() {
        this.objectMapper = createObjectMapper();
    }

    public AuditEvent deserialize(String json) throws IOException {
        AuditEvent auditEvent = objectMapper.readValue(json, AuditEvent.class);
        return auditEvent;
    }

    private ObjectMapper createObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        SimpleDateFormat sdf = new SimpleDateFormat(DATE_PATTERN);
        objectMapper.setDateFormat(sdf);
        return objectMapper;
    }
}
