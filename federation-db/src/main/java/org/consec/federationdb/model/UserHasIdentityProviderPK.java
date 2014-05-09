package org.consec.federationdb.model;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.io.Serializable;

@Embeddable
public class UserHasIdentityProviderPK implements Serializable {
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 36)
    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 36)
    @Column(name = "idp_id", nullable = false, length = 36)
    private String idpId;

    public UserHasIdentityProviderPK() {
    }

    public UserHasIdentityProviderPK(String userId, String idpId) {
        this.userId = userId;
        this.idpId = idpId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getIdpId() {
        return idpId;
    }

    public void setIdpId(String idpId) {
        this.idpId = idpId;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (userId != null ? userId.hashCode() : 0);
        hash += (idpId != null ? idpId.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof UserHasIdentityProviderPK)) {
            return false;
        }
        UserHasIdentityProviderPK other = (UserHasIdentityProviderPK) object;
        if ((this.userId == null && other.userId != null) || (this.userId != null && !this.userId.equals(other.userId))) {
            return false;
        }
        if ((this.idpId == null && other.idpId != null) || (this.idpId != null && !this.idpId.equals(other.idpId))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "org.consec.federationdb.model.UserHasIdentityProviderPK[ userId=" + userId + ", idpId=" + idpId + " ]";
    }

}
