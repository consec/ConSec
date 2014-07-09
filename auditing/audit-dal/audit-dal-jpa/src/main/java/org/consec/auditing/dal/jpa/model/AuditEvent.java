package org.consec.auditing.dal.jpa.model;

import javax.persistence.*;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "audit_event")
@NamedQueries({
        @NamedQuery(name = "AuditEvent.findAll", query = "SELECT a FROM AuditEvent a")})
public class AuditEvent implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "audit_event_id", nullable = false)
    private Integer auditEventId;
    @Size(max = 45)
    @Column(name = "eventType", length = 45)
    private String eventType;
    @Size(max = 255)
    @Column(name = "initiatorId", length = 255)
    private String initiatorId;
    @Size(max = 45)
    @Column(name = "initiatorType", length = 45)
    private String initiatorType;
    @Size(max = 45)
    @Column(name = "action", length = 45)
    private String action;
    @Column(name = "eventTime")
    @Temporal(TemporalType.TIMESTAMP)
    private Date eventTime;
    @Size(max = 255)
    @Column(name = "targetId", length = 255)
    private String targetId;
    @Size(max = 45)
    @Column(name = "targetType", length = 45)
    private String targetType;
    @Size(max = 45)
    @Column(name = "outcome", length = 45)
    private String outcome;
    @Size(max = 45)
    @Column(name = "severity", length = 45)
    private String severity;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "auditEvent")
    private List<Attachment> attachmentList;

    public AuditEvent() {
    }

    public AuditEvent(Integer auditEventId) {
        this.auditEventId = auditEventId;
    }

    public Integer getAuditEventId() {
        return auditEventId;
    }

    public void setAuditEventId(Integer auditEventId) {
        this.auditEventId = auditEventId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getInitiatorId() {
        return initiatorId;
    }

    public void setInitiatorId(String initiatorId) {
        this.initiatorId = initiatorId;
    }

    public String getInitiatorType() {
        return initiatorType;
    }

    public void setInitiatorType(String initiatorType) {
        this.initiatorType = initiatorType;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public Date getEventTime() {
        return eventTime;
    }

    public void setEventTime(Date eventTime) {
        this.eventTime = eventTime;
    }

    public String getTargetId() {
        return targetId;
    }

    public void setTargetId(String targetId) {
        this.targetId = targetId;
    }

    public String getTargetType() {
        return targetType;
    }

    public void setTargetType(String targetType) {
        this.targetType = targetType;
    }

    public String getOutcome() {
        return outcome;
    }

    public void setOutcome(String outcome) {
        this.outcome = outcome;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public List<Attachment> getAttachmentList() {
        return attachmentList;
    }

    public void setAttachmentList(List<Attachment> attachmentList) {
        this.attachmentList = attachmentList;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (auditEventId != null ? auditEventId.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof AuditEvent)) {
            return false;
        }
        AuditEvent other = (AuditEvent) object;
        if ((this.auditEventId == null && other.auditEventId != null) || (this.auditEventId != null && !this.auditEventId.equals(other.auditEventId))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "org.consec.auditing.dal.jpa.AuditEvent[ auditEventId=" + auditEventId + " ]";
    }

}
