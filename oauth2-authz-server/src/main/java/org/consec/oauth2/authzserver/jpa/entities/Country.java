package org.consec.oauth2.authzserver.jpa.entities;

import javax.persistence.*;
import java.io.Serializable;
import java.util.List;

@Entity
@Table(name = "country")
@NamedQueries({
        @NamedQuery(name = "Country.findAll", query = "SELECT c FROM Country c"),
        @NamedQuery(name = "Country.findByCode", query = "SELECT c FROM Country c WHERE c.code = :code"),
        @NamedQuery(name = "Country.findByName", query = "SELECT c FROM Country c WHERE c.name = :name")})
public class Country implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @Basic(optional = false)
    @Column(nullable = false, length = 2)
    private String code;
    @Basic(optional = false)
    @Column(nullable = false, length = 50)
    private String name;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "country")
    private List<CountryTrust> countryTrustList;


    public Country() {
    }

    public Country(String code) {
        this.code = code;
    }

    public Country(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<CountryTrust> getCountryTrustList() {
        return countryTrustList;
    }

    public void setCountryTrustList(List<CountryTrust> countryTrustList) {
        this.countryTrustList = countryTrustList;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (code != null ? code.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof Country)) {
            return false;
        }
        Country other = (Country) object;
        if ((this.code == null && other.code != null) || (this.code != null && !this.code.equals(other.code))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "org.consec.oauth2.authzserver.jpa.entities.Country[ code=" + code + " ]";
    }

}
