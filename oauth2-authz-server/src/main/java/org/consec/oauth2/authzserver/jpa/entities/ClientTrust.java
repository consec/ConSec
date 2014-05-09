package org.consec.oauth2.authzserver.jpa.entities;

import org.consec.oauth2.authzserver.jpa.enums.ClientTrustLevel;

import javax.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "client_trust")
@NamedQueries({
    @NamedQuery(name = "ClientTrust.findAll", query = "SELECT c FROM ClientTrust c"),
    @NamedQuery(name = "ClientTrust.findByOwnerId", query = "SELECT c FROM ClientTrust c WHERE c.clientTrustPK.ownerId = :ownerId"),
    @NamedQuery(name = "ClientTrust.findByClientId", query = "SELECT c FROM ClientTrust c WHERE c.clientTrustPK.clientId = :clientId"),
    @NamedQuery(name = "ClientTrust.findByOwnerIdOrgId",
            query = "SELECT c FROM ClientTrust c WHERE c.clientTrustPK.ownerId = :ownerId AND c.client.organization.id = :orgId")})
public class ClientTrust implements Serializable {
    private static final long serialVersionUID = 1L;
    @EmbeddedId
    protected ClientTrustPK clientTrustPK;
    @Basic(optional = false)
    @Column(name = "trust_level", nullable = false, length = 11)
    @Enumerated(EnumType.STRING)
    private ClientTrustLevel trustLevel;
    @JoinColumn(name = "client_id", referencedColumnName = "id", nullable = false, insertable = false, updatable = false)
    @ManyToOne(optional = false)
    private Client client;
    @JoinColumn(name = "owner_id", referencedColumnName = "id", nullable = false, insertable = false, updatable = false)
    @ManyToOne(optional = false)
    private Owner owner;

    public ClientTrust() {
    }

    public ClientTrust(ClientTrustPK clientTrustPK) {
        this.clientTrustPK = clientTrustPK;
    }

    public ClientTrust(int ownerId, int clientId) {
        this.clientTrustPK = new ClientTrustPK(ownerId, clientId);
    }

    public ClientTrustPK getClientTrustPK() {
        return clientTrustPK;
    }

    public void setClientTrustPK(ClientTrustPK clientTrustPK) {
        this.clientTrustPK = clientTrustPK;
    }

    public ClientTrustLevel getTrustLevel() {
        return trustLevel;
    }

    public void setTrustLevel(ClientTrustLevel trustLevel) {
        this.trustLevel = trustLevel;
    }

    public Client getClient() {
        return client;
    }

    public void setClient(Client client) {
        this.client = client;
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
        hash += (clientTrustPK != null ? clientTrustPK.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof ClientTrust)) {
            return false;
        }
        ClientTrust other = (ClientTrust) object;
        if ((this.clientTrustPK == null && other.clientTrustPK != null) || (this.clientTrustPK != null && !this.clientTrustPK.equals(other.clientTrustPK))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "org.consec.oauth2.authzserver.jpa.entities.ClientTrust[ clientTrustPK=" + clientTrustPK + " ]";
    }
    
}
