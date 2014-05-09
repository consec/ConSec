package org.consec.oauth2.authzserver.jpa.entities;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import java.io.Serializable;

@Embeddable
public class OrganizationTrustPK implements Serializable {
    @Basic(optional = false)
    @Column(name = "owner_id", nullable = false)
    private int ownerId;
    @Basic(optional = false)
    @Column(name = "organization_id", nullable = false)
    private int organizationId;

    public OrganizationTrustPK() {
    }

    public OrganizationTrustPK(int ownerId, int organizationId) {
        this.ownerId = ownerId;
        this.organizationId = organizationId;
    }

    public int getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(int ownerId) {
        this.ownerId = ownerId;
    }

    public int getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(int organizationId) {
        this.organizationId = organizationId;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (int) ownerId;
        hash += (int) organizationId;
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof OrganizationTrustPK)) {
            return false;
        }
        OrganizationTrustPK other = (OrganizationTrustPK) object;
        if (this.ownerId != other.ownerId) {
            return false;
        }
        if (this.organizationId != other.organizationId) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "org.consec.oauth2.authzserver.jpa.entities.OrganizationTrustPK[ ownerId=" + ownerId + ", organizationId=" + organizationId + " ]";
    }
    
}
