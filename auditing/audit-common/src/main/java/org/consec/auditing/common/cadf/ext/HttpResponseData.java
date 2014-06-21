package org.consec.auditing.common.cadf.ext;

import org.consec.auditing.common.cadf.Attachment;

public class HttpResponseData {
    private int statusCode;
    private String contentType;
    private String content;

    public HttpResponseData() {
    }

    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Attachment toAttachment() {
        Attachment attachment = new Attachment();
        attachment.setContentType("application/json");
        attachment.setName("http_response_data");
        attachment.setContent(this);
        return attachment;
    }
}
