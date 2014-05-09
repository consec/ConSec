package org.consec.federationdb.model;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.util.List;

@Entity
@Table(name = "attribute")
@NamedQueries({
        @NamedQuery(name = "Attribute.findAll", query = "SELECT a FROM Attribute a"),
        @NamedQuery(name = "Attribute.findByName", query = "SELECT a FROM Attribute a WHERE a.name = :name")
})
public class Attribute implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 36)
    @Column(name = "attribute_id", nullable = false, length = 36)
    private String attributeId;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 255)
    @Column(name = "name", nullable = false, length = 255)
    private String name;
    @Size(max = 255)
    @Column(name = "uri", length = 255)
    private String uri;
    @Size(max = 255)
    @Column(name = "description", length = 255)
    private String description;
    @Size(max = 255)
    @Column(name = "default_value", length = 255)
    private String defaultValue;
    @Size(max = 45)
    @Column(name = "reference", length = 45)
    private String reference;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "attribute")
    private List<UserHasAttribute> userHasAttributeList;

    public Attribute() {
    }

    public Attribute(String attributeId) {
        this.attributeId = attributeId;
    }

    public Attribute(String attributeId, String name) {
        this.attributeId = attributeId;
        this.name = name;
    }

    public String getAttributeId() {
        return attributeId;
    }

    public void setAttributeId(String attributeId) {
        this.attributeId = attributeId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public List<UserHasAttribute> getUserHasAttributeList() {
        return userHasAttributeList;
    }

    public void setUserHasAttributeList(List<UserHasAttribute> userHasAttributeList) {
        this.userHasAttributeList = userHasAttributeList;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (attributeId != null ? attributeId.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof Attribute)) {
            return false;
        }
        Attribute other = (Attribute) object;
        if ((this.attributeId == null && other.attributeId != null) || (this.attributeId != null && !this.attributeId.equals(other.attributeId))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "org.consec.federationdb.model.Attribute[ attributeId=" + attributeId + " ]";
    }

}
