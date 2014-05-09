package org.consec.oauth2.authzserver.jpa.entities;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "access_token", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"token"})})
@NamedQueries({
    @NamedQuery(name = "AccessToken.findAll", query = "SELECT a FROM AccessToken a"),
    @NamedQuery(name = "AccessToken.findById", query = "SELECT a FROM AccessToken a WHERE a.id = :id"),
    @NamedQuery(name = "AccessToken.findByToken", query = "SELECT a FROM AccessToken a WHERE a.token = :token")})
public class AccessToken implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(nullable = false)
    private Integer id;
    @Basic(optional = false)
    @Column(nullable = false, length = 256)
    private String token;
    @Basic(optional = false)
    @Column(name = "expire_time", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date expireTime;
    @Column(length = 1000)
    private String scope;
    @Basic(optional = false)
    @Column(name = "revoked", nullable = false)
    private boolean revoked;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "accessToken")
    private List<TokenInfoAccessLog> tokenInfoAccessLogList;
    @JoinColumn(name = "owner_id", referencedColumnName = "id", nullable = false)
    @ManyToOne(optional = false)
    private Owner owner;
    @JoinColumn(name = "client_id", referencedColumnName = "id", nullable = false)
    @ManyToOne(optional = false)
    private Client client;

    public AccessToken() {
    }

    public AccessToken(Integer id) {
        this.id = id;
    }

    public AccessToken(Integer id, String token, Date expireTime) {
        this.id = id;
        this.token = token;
        this.expireTime = expireTime;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public Date getExpireTime() {
        return expireTime;
    }

    public void setExpireTime(Date expireTime) {
        this.expireTime = expireTime;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public List<TokenInfoAccessLog> getTokenInfoAccessLogList() {
        return tokenInfoAccessLogList;
    }

    public void setTokenInfoAccessLogList(List<TokenInfoAccessLog> tokenInfoAccessLogList) {
        this.tokenInfoAccessLogList = tokenInfoAccessLogList;
    }

    public Owner getOwner() {
        return owner;
    }

    public void setOwner(Owner owner) {
        this.owner = owner;
    }

    public Client getClient() {
        return client;
    }

    public void setClient(Client client) {
        this.client = client;
    }

    public boolean isRevoked() {
        return revoked;
    }

    public void setRevoked(boolean revoked) {
        this.revoked = revoked;
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
        if (!(object instanceof AccessToken)) {
            return false;
        }
        AccessToken other = (AccessToken) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "org.consec.oauth2.authzserver.jpa.entities.AccessToken[ id=" + id + " ]";
    }
    
    public JSONObject toJson() throws JSONException {
        JSONObject o = new JSONObject();
        o.put("access_token", this.getToken());
        o.put("owner", owner.getUuid());
        o.put("client_id", client.getClientId());
        o.put("expire_time", this.getExpireTime());
        o.put("revoked", this.isRevoked());
        return o;
    }
}
