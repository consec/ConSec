package org.consec.auditing.common.utils;

import org.codehaus.jackson.map.ObjectMapper;
import org.consec.auditing.common.AuditMessage;

import java.io.IOException;
import java.text.SimpleDateFormat;

public class AuditEventDeserializer {
    private static final String DATE_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";

    private ObjectMapper objectMapper;

    public AuditEventDeserializer() {
        this.objectMapper = createObjectMapper();
    }

    public AuditMessage deserialize(String json) throws IOException {
        // TODO: use auditEventClass from audit message at deserialization
        AuditMessage auditMessage = objectMapper.readValue(json, AuditMessage.class);
        return auditMessage;
    }

    private ObjectMapper createObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        SimpleDateFormat sdf = new SimpleDateFormat(DATE_PATTERN);
        objectMapper.setDateFormat(sdf);
        return objectMapper;
    }
}
