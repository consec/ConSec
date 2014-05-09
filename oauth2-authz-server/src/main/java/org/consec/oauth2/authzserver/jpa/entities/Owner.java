package org.consec.oauth2.authzserver.jpa.entities;

import org.consec.oauth2.authzserver.jpa.enums.OwnerType;

import java.io.Serializable;
import java.util.List;
import javax.persistence.*;

@Entity
@Table(name = "owner",uniqueConstraints = {
    @UniqueConstraint(columnNames = {"uuid"})})
@NamedQueries({
    @NamedQuery(name = "Owner.findAll", query = "SELECT u FROM Owner u"),
    @NamedQuery(name = "Owner.findById", query = "SELECT u FROM Owner u WHERE u.id = :id"),
    @NamedQuery(name = "Owner.findByUuid", query = "SELECT u FROM Owner u WHERE u.uuid = :uuid")})
public class Owner implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(nullable = false)
    private Integer id;
    @Basic(optional = false)
    @Column(nullable = false, length = 255)
    private String uuid;
    @Basic(optional = false)
    @Column(name = "country_restriction", nullable = false)
    private boolean countryRestriction;
    @Basic(optional = false)
    @Column(name = "owner_type", nullable = false, length = 7)
    @Enumerated(EnumType.STRING)
    private OwnerType ownerType;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "owner")
    private List<ClientTrust> clientTrustList;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "owner")
    private List<OrganizationTrust> organizationTrustList;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "owner")
    private List<AuthzCode> authzCodeList;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "owner")
    private List<CountryTrust> countryTrustList;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "owner")
    private List<AccessToken> accessTokenList;

    public Owner() {
    }

    public Owner(Integer id) {
        this.id = id;
    }

    public Owner(Integer id, String uuid) {
        this.id = id;
        this.uuid = uuid;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public boolean getCountryRestriction() {
        return countryRestriction;
    }

    public void setCountryRestriction(boolean countryRestriction) {
        this.countryRestriction = countryRestriction;
    }

    public OwnerType getOwnerType() {
        return ownerType;
    }

    public void setOwnerType(OwnerType ownerType) {
        this.ownerType = ownerType;
    }

    public List<ClientTrust> getClientTrustList() {
        return clientTrustList;
    }

    public void setClientTrustList(List<ClientTrust> clientTrustList) {
        this.clientTrustList = clientTrustList;
    }

    public List<OrganizationTrust> getOrganizationTrustList() {
        return organizationTrustList;
    }

    public void setOrganizationTrustList(List<OrganizationTrust> organizationTrustList) {
        this.organizationTrustList = organizationTrustList;
    }

    public List<AuthzCode> getAuthzCodeList() {
        return authzCodeList;
    }

    public void setAuthzCodeList(List<AuthzCode> authzCodeList) {
        this.authzCodeList = authzCodeList;
    }

    public List<CountryTrust> getCountryTrustList() {
        return countryTrustList;
    }

    public void setCountryTrustList(List<CountryTrust> countryTrustList) {
        this.countryTrustList = countryTrustList;
    }

    public List<AccessToken> getAccessTokenList() {
        return accessTokenList;
    }

    public void setAccessTokenList(List<AccessToken> accessTokenList) {
        this.accessTokenList = accessTokenList;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (id != null ? id.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof Owner)) {
            return false;
        }
        Owner other = (Owner) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "org.consec.oauth2.authzserver.jpa.entities.Owner[ id=" + id + " ]";
    }
    
}
