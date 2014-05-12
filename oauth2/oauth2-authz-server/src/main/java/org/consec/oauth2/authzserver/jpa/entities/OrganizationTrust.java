package org.consec.oauth2.authzserver.jpa.entities;

import org.consec.oauth2.authzserver.jpa.enums.OrganizationTrustLevel;

import javax.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "organization_trust")
@NamedQueries({
    @NamedQuery(name = "OrganizationTrust.findAll", query = "SELECT o FROM OrganizationTrust o"),
    @NamedQuery(name = "OrganizationTrust.findByOwnerId", query = "SELECT o FROM OrganizationTrust o WHERE o.organizationTrustPK.ownerId = :ownerId"),
    @NamedQuery(name = "OrganizationTrust.findByOrganizationId", query = "SELECT o FROM OrganizationTrust o WHERE o.organizationTrustPK.organizationId = :organizationId")})
public class OrganizationTrust implements Serializable {
    private static final long serialVersionUID = 1L;
    @EmbeddedId
    protected OrganizationTrustPK organizationTrustPK;
    @Basic(optional = false)
    @Column(name = "trust_level", nullable = false, length = 6)
    @Enumerated(EnumType.STRING)
    private OrganizationTrustLevel trustLevel;
    @JoinColumn(name = "organization_id", referencedColumnName = "id", nullable = false, insertable = false, updatable = false)
    @ManyToOne(optional = false)
    private Organization organization;
    @JoinColumn(name = "owner_id", referencedColumnName = "id", nullable = false, insertable = false, updatable = false)
    @ManyToOne(optional = false)
    private Owner owner;

    public OrganizationTrust() {
    }

    public OrganizationTrust(OrganizationTrustPK organizationTrustPK) {
        this.organizationTrustPK = organizationTrustPK;
    }

    public OrganizationTrust(int ownerId, int organizationId) {
        this.organizationTrustPK = new OrganizationTrustPK(ownerId, organizationId);
    }

    public OrganizationTrustPK getOrganizationTrustPK() {
        return organizationTrustPK;
    }

    public void setOrganizationTrustPK(OrganizationTrustPK organizationTrustPK) {
        this.organizationTrustPK = organizationTrustPK;
    }

    public OrganizationTrustLevel getTrustLevel() {
        return trustLevel;
    }

    public void setTrustLevel(OrganizationTrustLevel trustLevel) {
        this.trustLevel = trustLevel;
    }

    public Organization getOrganization() {
        return organization;
    }

    public void setOrganization(Organization organization) {
        this.organization = organization;
    }

    public Owner getOwner() {
        return owner;
    }

    public void setOwner(Owner owner) {
        this.owner = owner;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (organizationTrustPK != null ? organizationTrustPK.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof OrganizationTrust)) {
            return false;
        }
        OrganizationTrust other = (OrganizationTrust) object;
        if ((this.organizationTrustPK == null && other.organizationTrustPK != null) || (this.organizationTrustPK != null && !this.organizationTrustPK.equals(other.organizationTrustPK))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "org.consec.oauth2.authzserver.jpa.entities.OrganizationTrust[ organizationTrustPK=" + organizationTrustPK + " ]";
    }
    
}
