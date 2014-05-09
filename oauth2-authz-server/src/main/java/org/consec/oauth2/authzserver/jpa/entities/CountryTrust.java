package org.consec.oauth2.authzserver.jpa.entities;

import javax.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "country_trust")
@NamedQueries({
    @NamedQuery(name = "CountryTrust.findAll", query = "SELECT c FROM CountryTrust c"),
    @NamedQuery(name = "CountryTrust.findByOwnerId", query = "SELECT c FROM CountryTrust c WHERE c.countryTrustPK.ownerId = :ownerId"),
    @NamedQuery(name = "CountryTrust.findByCountryCode", query = "SELECT c FROM CountryTrust c WHERE c.countryTrustPK.countryCode = :countryCode")})
public class CountryTrust implements Serializable {
    private static final long serialVersionUID = 1L;
    @EmbeddedId
    protected CountryTrustPK countryTrustPK;
    @Basic(optional = false)
    @Column(name = "is_trusted", nullable = false)
    private boolean isTrusted;
    @JoinColumn(name = "country_code", referencedColumnName = "code", nullable = false, insertable = false, updatable = false)
    @ManyToOne(optional = false)
    private Country country;
    @JoinColumn(name = "owner_id", referencedColumnName = "id", nullable = false, insertable = false, updatable = false)
    @ManyToOne(optional = false)
    private Owner owner;

    public CountryTrust() {
    }

    public CountryTrust(CountryTrustPK countryTrustPK) {
        this.countryTrustPK = countryTrustPK;
    }

    public CountryTrust(CountryTrustPK countryTrustPK, boolean isTrusted) {
        this.countryTrustPK = countryTrustPK;
        this.isTrusted = isTrusted;
    }

    public CountryTrust(int ownerId, String countryCode) {
        this.countryTrustPK = new CountryTrustPK(ownerId, countryCode);
    }

    public CountryTrustPK getCountryTrustPK() {
        return countryTrustPK;
    }

    public void setCountryTrustPK(CountryTrustPK countryTrustPK) {
        this.countryTrustPK = countryTrustPK;
    }

    public boolean getIsTrusted() {
        return isTrusted;
    }

    public void setIsTrusted(boolean isTrusted) {
        this.isTrusted = isTrusted;
    }

    public Country getCountry() {
        return country;
    }

    public void setCountry(Country country) {
        this.country = country;
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
        hash += (countryTrustPK != null ? countryTrustPK.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof CountryTrust)) {
            return false;
        }
        CountryTrust other = (CountryTrust) object;
        if ((this.countryTrustPK == null && other.countryTrustPK != null) || (this.countryTrustPK != null && !this.countryTrustPK.equals(other.countryTrustPK))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "org.consec.oauth2.authzserver.jpa.entities.CountryTrust[ countryTrustPK=" + countryTrustPK + " ]";
    }
    
}
