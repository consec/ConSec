package org.consec.auditing.client;

import org.consec.auditing.common.auditevent.AuditEvent;

public interface Auditor {

    public void audit(AuditEvent auditEvent);

    public void close();

}
