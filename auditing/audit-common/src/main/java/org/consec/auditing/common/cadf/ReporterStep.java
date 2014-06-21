package org.consec.auditing.common.cadf;

import java.util.Date;
import java.util.List;

public class ReporterStep {
    private String role;
    private Resource reporter;
    private String reporterId;
    private Date reporterTime;
    private List<Attachment> attachments;

    public ReporterStep() {
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public Resource getReporter() {
        return reporter;
    }

    public void setReporter(Resource reporter) {
        this.reporter = reporter;
    }

    public String getReporterId() {
        return reporterId;
    }

    public void setReporterId(String reporterId) {
        this.reporterId = reporterId;
    }

    public Date getReporterTime() {
        return reporterTime;
    }

    public void setReporterTime(Date reporterTime) {
        this.reporterTime = reporterTime;
    }

    public List<Attachment> getAttachments() {
        return attachments;
    }

    public void setAttachments(List<Attachment> attachments) {
        this.attachments = attachments;
    }
}
