package org.consec.auditing.common.cadf;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class CADFEventRecord {
    private String typeURI = "http://schemas.dmtf.org/cloud/audit/1.0/event";
    private String id;
    private EventType eventType;
    private Date eventTime;
    private String action;
    private Outcome outcome;
    private Resource initiator;
    private String initiatorId;
    private Resource target;
    private String targetId;
    private Reason reason;
    private String severity;
    private List<Measurement> measurements;
    private List<String> tags;
    private List<Attachment> attachments;

    public CADFEventRecord() {
    }

    public String getTypeURI() {
        return typeURI;
    }

    public String getId() {
        return id;
    }

    /**
     * The unique identifier of the CADF Event Record.
     *
     * @param id
     */
    public void setId(String id) {
        this.id = id;
    }

    public EventType getEventType() {
        return eventType;
    }

    public void setEventType(EventType eventType) {
        this.eventType = eventType;
    }

    public Date getEventTime() {
        return eventTime;
    }

    /**
     * The OBSERVER's best estimate as to the time the Actual Event occurred or began (note that this may differ
     * significantly from the time at which the OBSERVER is processing the Event Record).
     *
     * @param eventTime
     */
    public void setEventTime(Date eventTime) {
        this.eventTime = eventTime;
    }

    public String getAction() {
        return action;
    }

    /**
     * The operation or activity the INITIATOR has performed, attempted to perform or has pending against the event's
     * TARGET, according to the OBSERVER
     *
     * @param action
     */
    public void setAction(String action) {
        this.action = action;
    }

    public Outcome getOutcome() {
        return outcome;
    }

    /**
     * The result or status of the ACTION of the observed event.
     *
     * @param outcome
     */
    public void setOutcome(Outcome outcome) {
        this.outcome = outcome;
    }

    public Resource getInitiator() {
        return initiator;
    }

    /**
     * The RESOURCE that initiated, originated, or instigated the event's ACTION, according to the OBSERVER.
     *
     * @param initiator
     */
    public void setInitiator(Resource initiator) {
        this.initiator = initiator;
    }

    public String getInitiatorId() {
        return initiatorId;
    }

    public void setInitiatorId(String initiatorId) {
        this.initiatorId = initiatorId;
    }

    public Resource getTarget() {
        return target;
    }

    /**
     * The RESOURCE against which the ACTION of a CADF Event Record was performed, was attempted, or is pending.
     *
     * @param target
     */
    public void setTarget(Resource target) {
        this.target = target;
    }

    public String getTargetId() {
        return targetId;
    }

    public void setTargetId(String targetId) {
        this.targetId = targetId;
    }

    public Reason getReason() {
        return reason;
    }

    public void setReason(Reason reason) {
        this.reason = reason;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public List<Measurement> getMeasurements() {
        return measurements;
    }

    public void setMeasurements(List<Measurement> measurements) {
        this.measurements = measurements;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public List<Attachment> getAttachments() {
        return attachments;
    }

    public void setAttachments(List<Attachment> attachments) {
        this.attachments = attachments;
    }

    public void addAttachment(Attachment attachment) {
        if (attachments == null) {
            attachments = new ArrayList<Attachment>();
        }
        attachments.add(attachment);
    }
}

