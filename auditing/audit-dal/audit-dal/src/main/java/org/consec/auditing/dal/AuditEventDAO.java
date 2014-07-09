package org.consec.auditing.dal;

import org.consec.auditing.common.auditevent.AuditEvent;

public interface AuditEventDAO {

    public void save(AuditEvent auditEvent);

    public void retrieve(int auditEventId);
}
