package org.consec.auditing.common.auditevent;

public class Attachment {
    private String name;
    private String contentType;
    private Object content;

    public Attachment() {
    }

    public Attachment(String name, String contentType, Object content) {
        this.name = name;
        this.contentType = contentType;
        this.content = content;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public Object getContent() {
        return content;
    }

    public void setContent(Object content) {
        this.content = content;
    }
}
