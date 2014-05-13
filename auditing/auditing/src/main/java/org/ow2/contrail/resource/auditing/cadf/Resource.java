package org.ow2.contrail.resource.auditing.cadf;

import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonSubTypes;
import org.codehaus.jackson.annotate.JsonTypeInfo;
import org.ow2.contrail.resource.auditing.cadf.ext.Initiator;

import java.util.List;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "typeURI")
@JsonSubTypes({
        @JsonSubTypes.Type(value = Resource.class, name = Resource.TYPE_URI),
        @JsonSubTypes.Type(value = Initiator.class, name = Initiator.TYPE_URI)})
public class Resource {
    public static final String TYPE_URI = "cadf:resource";

    protected String id;
    protected String typeURI;
    protected String name;
    protected String ref;
    protected String domain;
    protected Geolocation geolocation;
    protected String geolocationId;
    protected List<Attachment> attachments;

    public Resource() {
        typeURI = TYPE_URI;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @JsonIgnore
    public String getTypeURI() {
        return typeURI;
    }

    public void setTypeURI(String typeURI) {
        this.typeURI = typeURI;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRef() {
        return ref;
    }

    public void setRef(String ref) {
        this.ref = ref;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public Geolocation getGeolocation() {
        return geolocation;
    }

    public void setGeolocation(Geolocation geolocation) {
        this.geolocation = geolocation;
    }

    public String getGeolocationId() {
        return geolocationId;
    }

    public void setGeolocationId(String geolocationId) {
        this.geolocationId = geolocationId;
    }

    public List<Attachment> getAttachments() {
        return attachments;
    }

    public void setAttachments(List<Attachment> attachments) {
        this.attachments = attachments;
    }
}
