package org.consec.federationdb.model;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.io.Serializable;

@Entity
@Table(name = "user_has_identity_provider")
@NamedQueries({
        @NamedQuery(name = "UserHasIdentityProvider.findAll", query = "SELECT u FROM UserHasIdentityProvider u"),
        @NamedQuery(name = "UserHasIdentityProvider.findByIdentity", query = "SELECT u FROM UserHasIdentityProvider u WHERE u.identity = :identity")
})
public class UserHasIdentityProvider implements Serializable {
    private static final long serialVersionUID = 1L;
    @EmbeddedId
    protected UserHasIdentityProviderPK userHasIdentityProviderPK;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 255)
    @Column(name = "identity", nullable = false, length = 255)
    private String identity;
    @Lob
    @Size(max = 65535)
    @Column(name = "attributes", length = 65535)
    private String attributes;
    @JoinColumn(name = "idp_id", referencedColumnName = "idp_id", nullable = false, insertable = false, updatable = false)
    @ManyToOne(optional = false)
    private IdentityProvider identityProvider;
    @JoinColumn(name = "user_id", referencedColumnName = "user_id", nullable = false, insertable = false, updatable = false)
    @ManyToOne(optional = false)
    private User user;

    public UserHasIdentityProvider() {
    }

    public UserHasIdentityProvider(UserHasIdentityProviderPK userHasIdentityProviderPK) {
        this.userHasIdentityProviderPK = userHasIdentityProviderPK;
    }

    public UserHasIdentityProvider(UserHasIdentityProviderPK userHasIdentityProviderPK, String identity) {
        this.userHasIdentityProviderPK = userHasIdentityProviderPK;
        this.identity = identity;
    }

    public UserHasIdentityProvider(String userId, String idpId) {
        this.userHasIdentityProviderPK = new UserHasIdentityProviderPK(userId, idpId);
    }

    public UserHasIdentityProviderPK getUserHasIdentityProviderPK() {
        return userHasIdentityProviderPK;
    }

    public void setUserHasIdentityProviderPK(UserHasIdentityProviderPK userHasIdentityProviderPK) {
        this.userHasIdentityProviderPK = userHasIdentityProviderPK;
    }

    public String getIdentity() {
        return identity;
    }

    public void setIdentity(String identity) {
        this.identity = identity;
    }

    public String getAttributes() {
        return attributes;
    }

    public void setAttributes(String attributes) {
        this.attributes = attributes;
    }

    public IdentityProvider getIdentityProvider() {
        return identityProvider;
    }

    public void setIdentityProvider(IdentityProvider identityProvider) {
        this.identityProvider = identityProvider;
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
        hash += (userHasIdentityProviderPK != null ? userHasIdentityProviderPK.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof UserHasIdentityProvider)) {
            return false;
        }
        UserHasIdentityProvider other = (UserHasIdentityProvider) object;
        if ((this.userHasIdentityProviderPK == null && other.userHasIdentityProviderPK != null) || (this.userHasIdentityProviderPK != null && !this.userHasIdentityProviderPK.equals(other.userHasIdentityProviderPK))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "org.consec.federationdb.model.UserHasIdentityProvider[ userHasIdentityProviderPK=" + userHasIdentityProviderPK + " ]";
    }

}
