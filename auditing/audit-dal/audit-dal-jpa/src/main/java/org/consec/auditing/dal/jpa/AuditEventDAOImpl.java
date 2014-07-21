package org.consec.auditing.dal.jpa;

import org.consec.auditing.common.auditevent.Attachment;
import org.consec.auditing.common.auditevent.AuditEvent;
import org.consec.auditing.dal.AuditEventDAO;
import org.consec.auditing.dal.jpa.utils.EMF;

import javax.persistence.EntityManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AuditEventDAOImpl implements AuditEventDAO {
    @Override
    public void save(AuditEvent auditEvent) {
        org.consec.auditing.dal.jpa.model.AuditEvent auditEventJpa =
                new org.consec.auditing.dal.jpa.model.AuditEvent();

        auditEventJpa.setAction(auditEvent.getAction());
        auditEventJpa.setEventTime(auditEvent.getEventTime());
        auditEventJpa.setEventType(auditEvent.getEventType());
        auditEventJpa.setInitiatorType(auditEvent.getInitiator().getType());
        auditEventJpa.setInitiatorId(auditEvent.getInitiator().getId());
        auditEventJpa.setTargetType(auditEvent.getTarget().getType());
        auditEventJpa.setTargetId(auditEvent.getTarget().getId());
        auditEventJpa.setSeverity(auditEvent.getSeverity().name());
        auditEventJpa.setOutcome(auditEvent.getOutcome().name());

        if (auditEvent.getAttachments() != null) {
            List<org.consec.auditing.dal.jpa.model.Attachment> attachmentListJpa =
                    new ArrayList<org.consec.auditing.dal.jpa.model.Attachment>();
            for (Map.Entry<String, Attachment> entry : auditEvent.getAttachments().entrySet()) {
                Attachment attachment = entry.getValue();
                org.consec.auditing.dal.jpa.model.Attachment attachmentJpa =
                        new org.consec.auditing.dal.jpa.model.Attachment();
                attachmentJpa.setName(attachment.getName());
                attachmentJpa.setContentType(attachment.getContentType());
                // TODO: support for other attachment content types
                attachmentJpa.setContent((String) attachment.getContent());

                attachmentListJpa.add(attachmentJpa);
            }
            auditEventJpa.setAttachmentList(attachmentListJpa);
        }

        EntityManager em = EMF.createEntityManager();
        try {
            em.getTransaction().begin();
            em.persist(auditEventJpa);
            em.getTransaction().commit();
        }
        finally {
            EMF.closeEntityManager(em);
        }
    }

    @Override
    public void retrieve(int auditEventId) {

    }
}
