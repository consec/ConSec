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
     * The resource that initiated, originated or instigated the event action. Typically,
     * the initiating resource is either a 653 user or service that can be identified or described by the system in
     * which the event occurs.
     */
    private Initiator initiator;

    /**
     * The action (verb) performed by the event initiator against the event target resource
     */
    public String action;

    /**
     * The time the event occurred or began
     */
    private Date eventTime;

    /**
     * The resource against which the action was performed.
     */
    private Target target;

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

    public Initiator getInitiator() {
        return initiator;
    }

    public void setInitiator(Initiator initiator) {
        this.initiator = initiator;
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

    public Target getTarget() {
        return target;
    }

    public void setTarget(Target target) {
        this.target = target;
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

    public void addAttachment(Attachment attachment) {
        if (attachments == null) {
            attachments = new HashMap<String, Attachment>();
        }
        attachments.put(attachment.getName(), attachment);
    }
}
