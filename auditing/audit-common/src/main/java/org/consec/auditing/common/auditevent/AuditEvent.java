package org.consec.auditing.common.auditevent;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class AuditEvent {

    /**
     * The type (category) of the event.
     */
    private String eventType;

    /**
     * The ID of resource that initiated, originated or instigated the event action. Typically,
     * the initiating resource is either a 653 user or service that can be identified or described by the system in
     * which the event occurs.
     */
    private String initiatorId;

    /**
     * The type of the initiator resource identified by the {@link AuditEvent#initiatorId}
     */
    private String initiatorType;

    /**
     * The action (verb) performed by the event initiator against the event target resource
     */
    public String action;

    /**
     * The time the event occurred or began
     */
    private Date eventTime;

    /**
     * The ID of the resource against which the action was performed.
     */
    private String targetId;

    /**
     * The type of the target resource identified by the {@link AuditEvent#targetId}
     */
    private String targetType;

    /**
     * The result or status of the action of the observed event.
     */
    private Outcome outcome;

    /**
     * The severity level of the observed event.
     */
    private Severity severity;

    /**
     * The event attachments with domain-specific detailed information.
     */
    private Map<String, Attachment> attachments;


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

    public Outcome getOutcome() {
        return outcome;
    }

    public void setOutcome(Outcome outcome) {
        this.outcome = outcome;
    }

    public Severity getSeverity() {
        return severity;
    }

    public void setSeverity(Severity severity) {
        this.severity = severity;
    }

    public Map<String, Attachment> getAttachments() {
        return attachments;
    }

    public Attachment getAttachment(String attachmentName) {
        return attachments.get(attachmentName);
    }

    public void addAttachment(String attachmentName, Attachment attachment) {
        if (attachments == null) {
            attachments = new HashMap<String, Attachment>();
        }
        attachments.put(attachmentName, attachment);
    }
}
