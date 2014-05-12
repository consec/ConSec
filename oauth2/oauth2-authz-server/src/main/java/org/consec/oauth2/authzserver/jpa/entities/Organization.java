package org.consec.oauth2.authzserver.jpa.entities;

import javax.persistence.*;
import java.io.Serializable;
import java.util.List;

@Entity
@Table(name = "organization")
@NamedQueries({
        @NamedQuery(name = "Organization.findAll", query = "SELECT o FROM Organization o"),
        @NamedQuery(name = "Organization.findById", query = "SELECT o FROM Organization o WHERE o.id = :id"),
        @NamedQuery(name = "Organization.findByName", query = "SELECT o FROM Organization o WHERE o.name = :name")})
public class Organization implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(nullable = false)
    private Integer id;
    @Column(length = 100)
    private String name;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "organization")
    private List<Client> clientList;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "organization")
    private List<OrganizationTrust> organizationTrustList;

    public Organization() {
    }

    public Organization(Integer id) {
        this.id = id;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Client> getClientList() {
        return clientList;
    }

    public void setClientList(List<Client> clientList) {
        this.clientList = clientList;
    }

    public List<OrganizationTrust> getOrganizationTrustList() {
        return organizationTrustList;
    }

    public void setOrganizationTrustList(List<OrganizationTrust> organizationTrustList) {
        this.organizationTrustList = organizationTrustList;
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
        if (!(object instanceof Organization)) {
            return false;
        }
        Organization other = (Organization) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "org.consec.oauth2.authzserver.jpa.entities.Organization[ id=" + id + " ]";
    }

}
