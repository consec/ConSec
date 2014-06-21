package org.consec.auditing.common.cadf.ext;

import org.consec.auditing.common.cadf.Attachment;

public class HttpRequestData {
    private String method;
    private String url;
    private String contentType;
    private String content;

    public HttpRequestData() {
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
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
        attachment.setName("http_request_data");
        attachment.setContent(this);
        return attachment;
    }
}
