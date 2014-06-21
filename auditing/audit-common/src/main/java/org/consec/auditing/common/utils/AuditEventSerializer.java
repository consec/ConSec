package org.consec.auditing.common.utils;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.consec.auditing.common.AuditEvent;
import org.consec.auditing.common.AuditMessage;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class AuditEventSerializer {
    private static final String DATE_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";

    private ObjectMapper objectMapper;

    public AuditEventSerializer() {
        this.objectMapper = createObjectMapper();
    }

    public String serialize(AuditEvent auditEvent) throws IOException {
        AuditMessage auditMessage = new AuditMessage();
        auditMessage.setAuditEvent(auditEvent);
        auditMessage.setAuditEventClass(auditEvent.getClass().getName());
        auditMessage.setTimestamp(new Date());
        return objectMapper.writeValueAsString(auditMessage);
    }

    private ObjectMapper createObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setSerializationInclusion(JsonSerialize.Inclusion.NON_NULL);
        objectMapper.configure(SerializationConfig.Feature.WRITE_DATES_AS_TIMESTAMPS, false);
        SimpleDateFormat sdf = new SimpleDateFormat(DATE_PATTERN);
        objectMapper.setDateFormat(sdf);
        return objectMapper;
    }
}
