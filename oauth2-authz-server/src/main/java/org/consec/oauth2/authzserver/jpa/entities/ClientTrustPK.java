package org.consec.oauth2.authzserver.jpa.entities;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import java.io.Serializable;

@Embeddable
public class ClientTrustPK implements Serializable {
    @Basic(optional = false)
    @Column(name = "owner_id", nullable = false)
    private int ownerId;
    @Basic(optional = false)
    @Column(name = "client_id", nullable = false)
    private int clientId;

    public ClientTrustPK() {
    }

    public ClientTrustPK(int ownerId, int clientId) {
        this.ownerId = ownerId;
        this.clientId = clientId;
    }

    public int getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(int ownerId) {
        this.ownerId = ownerId;
    }

    public int getClientId() {
        return clientId;
    }

    public void setClientId(int clientId) {
        this.clientId = clientId;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (int) ownerId;
        hash += (int) clientId;
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof ClientTrustPK)) {
            return false;
        }
        ClientTrustPK other = (ClientTrustPK) object;
        if (this.ownerId != other.ownerId) {
            return false;
        }
        if (this.clientId != other.clientId) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "org.consec.oauth2.authzserver.jpa.entities.ClientTrustPK[ ownerId=" + ownerId + ", clientId=" + clientId + " ]";
    }
    
}
