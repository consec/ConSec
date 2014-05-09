package org.consec.oauth2.authzserver.jpa.entities;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import java.io.Serializable;

@Embeddable
public class CountryTrustPK implements Serializable {
    @Basic(optional = false)
    @Column(name = "owner_id", nullable = false)
    private int ownerId;
    @Basic(optional = false)
    @Column(name = "country_code", nullable = false, length = 2)
    private String countryCode;

    public CountryTrustPK() {
    }

    public CountryTrustPK(int ownerId, String countryCode) {
        this.ownerId = ownerId;
        this.countryCode = countryCode;
    }

    public int getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(int ownerId) {
        this.ownerId = ownerId;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (int) ownerId;
        hash += (countryCode != null ? countryCode.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof CountryTrustPK)) {
            return false;
        }
        CountryTrustPK other = (CountryTrustPK) object;
        if (this.ownerId != other.ownerId) {
            return false;
        }
        if ((this.countryCode == null && other.countryCode != null) || (this.countryCode != null && !this.countryCode.equals(other.countryCode))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "org.consec.oauth2.authzserver.jpa.entities.CountryTrustPK[ ownerId=" + ownerId + ", countryCode=" + countryCode + " ]";
    }
    
}
