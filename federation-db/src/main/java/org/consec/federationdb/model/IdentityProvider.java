package org.consec.federationdb.model;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.util.List;

@Entity
@Table(name = "identity_provider")
@NamedQueries({
        @NamedQuery(name = "IdentityProvider.findAll", query = "SELECT i FROM IdentityProvider i")})
public class IdentityProvider implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 36)
    @Column(name = "idp_id", nullable = false, length = 36)
    private String idpId;
    @Size(max = 255)
    @Column(name = "name", length = 255)
    private String name;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 255)
    @Column(name = "uri", nullable = false, length = 255)
    private String uri;
    @Size(max = 255)
    @Column(name = "description", length = 255)
    private String description;
    @Lob
    @Size(max = 65535)
    @Column(name = "attributes", length = 65535)
    private String attributes;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "identityProvider")
    private List<UserHasIdentityProvider> userHasIdentityProviderList;

    public IdentityProvider() {
    }

    public IdentityProvider(String idpId) {
        this.idpId = idpId;
    }

    public IdentityProvider(String idpId, String uri) {
        this.idpId = idpId;
        this.uri = uri;
    }

    public String getIdpId() {
        return idpId;
    }

    public void setIdpId(String idpId) {
        this.idpId = idpId;
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

    public String getAttributes() {
        return attributes;
    }

    public void setAttributes(String attributes) {
        this.attributes = attributes;
    }

    public List<UserHasIdentityProvider> getUserHasIdentityProviderList() {
        return userHasIdentityProviderList;
    }

    public void setUserHasIdentityProviderList(List<UserHasIdentityProvider> userHasIdentityProviderList) {
        this.userHasIdentityProviderList = userHasIdentityProviderList;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (idpId != null ? idpId.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof IdentityProvider)) {
            return false;
        }
        IdentityProvider other = (IdentityProvider) object;
        if ((this.idpId == null && other.idpId != null) || (this.idpId != null && !this.idpId.equals(other.idpId))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "org.consec.federationdb.model.IdentityProvider[ idpId=" + idpId + " ]";
    }

}
