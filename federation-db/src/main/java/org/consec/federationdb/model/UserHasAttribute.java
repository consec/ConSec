package org.consec.federationdb.model;

import javax.persistence.*;
import javax.validation.constraints.Size;
import java.io.Serializable;

@Entity
@Table(name = "user_has_attribute")
@NamedQueries({
        @NamedQuery(name = "UserHasAttribute.findAll", query = "SELECT u FROM UserHasAttribute u"),
        @NamedQuery(name = "UserHasAttribute.findByAttributeId", query = "SELECT u FROM UserHasAttribute u WHERE u.userHasAttributePK.attributeId = :attributeId")
})
public class UserHasAttribute implements Serializable {
    private static final long serialVersionUID = 1L;
    @EmbeddedId
    protected UserHasAttributePK userHasAttributePK;
    @Size(max = 255)
    @Column(name = "value", length = 255)
    private String value;
    @Column(name = "referenceId")
    private Integer referenceId;
    @JoinColumn(name = "attribute_id", referencedColumnName = "attribute_id", nullable = false, insertable = false, updatable = false)
    @ManyToOne(optional = false)
    private Attribute attribute;
    @JoinColumn(name = "user_id", referencedColumnName = "user_id", nullable = false, insertable = false, updatable = false)
    @ManyToOne(optional = false)
    private User user;

    public UserHasAttribute() {
    }

    public UserHasAttribute(UserHasAttributePK userHasAttributePK) {
        this.userHasAttributePK = userHasAttributePK;
    }

    public UserHasAttribute(String userId, String attributeId) {
        this.userHasAttributePK = new UserHasAttributePK(userId, attributeId);
    }

    public UserHasAttributePK getUserHasAttributePK() {
        return userHasAttributePK;
    }

    public void setUserHasAttributePK(UserHasAttributePK userHasAttributePK) {
        this.userHasAttributePK = userHasAttributePK;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public Integer getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(Integer referenceId) {
        this.referenceId = referenceId;
    }

    public Attribute getAttribute() {
        return attribute;
    }

    public void setAttribute(Attribute attribute) {
        this.attribute = attribute;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (userHasAttributePK != null ? userHasAttributePK.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof UserHasAttribute)) {
            return false;
        }
        UserHasAttribute other = (UserHasAttribute) object;
        if ((this.userHasAttributePK == null && other.userHasAttributePK != null) || (this.userHasAttributePK != null && !this.userHasAttributePK.equals(other.userHasAttributePK))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "org.consec.federationdb.model.UserHasAttribute[ userHasAttributePK=" + userHasAttributePK + " ]";
    }

}
