package org.consec.auditing.common;

import java.util.Date;

public class AuditMessage {
    private AuditEvent auditEvent;
    public String auditEventClass;
    private Date timestamp;

    public AuditEvent getAuditEvent() {
        return auditEvent;
    }

    public void setAuditEvent(AuditEvent auditEvent) {
        this.auditEvent = auditEvent;
    }

    public String getAuditEventClass() {
        return auditEventClass;
    }

    public void setAuditEventClass(String auditEventClass) {
        this.auditEventClass = auditEventClass;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }
}
