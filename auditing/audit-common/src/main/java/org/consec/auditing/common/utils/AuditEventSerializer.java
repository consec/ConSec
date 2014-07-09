package org.consec.auditing.common.utils;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.consec.auditing.common.auditevent.AuditEvent;

import java.io.IOException;
import java.text.SimpleDateFormat;

public class AuditEventSerializer {
    private static final String DATE_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";

    private ObjectMapper objectMapper;

    public AuditEventSerializer() {
        this.objectMapper = createObjectMapper();
    }

    public String serialize(AuditEvent auditEvent) throws IOException {
        return objectMapper.writeValueAsString(auditEvent);
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
