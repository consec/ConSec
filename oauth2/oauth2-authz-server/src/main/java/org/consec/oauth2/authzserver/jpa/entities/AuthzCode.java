package org.consec.oauth2.authzserver.jpa.entities;

import java.io.Serializable;
import java.util.Date;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.UniqueConstraint;

@Entity
@Table(name = "authz_code", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"code"})})
@NamedQueries({
    @NamedQuery(name = "AuthzCode.findAll", query = "SELECT a FROM AuthzCode a"),
    @NamedQuery(name = "AuthzCode.findById", query = "SELECT a FROM AuthzCode a WHERE a.id = :id"),
    @NamedQuery(name = "AuthzCode.findByCode", query = "SELECT a FROM AuthzCode a WHERE a.code = :code")})
public class AuthzCode implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(nullable = false)
    private Integer id;
    @Basic(optional = false)
    @Column(nullable = false, length = 256)
    private String code;
    @Basic(optional = false)
    @Column(name = "redirect_uri", nullable = false, length = 256)
    private String redirectUri;
    @Basic(optional = false)
    @Column(name = "expire_time", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date expireTime;
    @Column(length = 1000)
    private String scope;
    @JoinColumn(name = "owner_id", referencedColumnName = "id", nullable = false)
    @ManyToOne(optional = false)
    private Owner owner;
    @JoinColumn(name = "client_id", referencedColumnName = "id", nullable = false)
    @ManyToOne(optional = false)
    private Client client;

    public AuthzCode() {
    }

    public AuthzCode(Integer id) {
        this.id = id;
    }

    public AuthzCode(Integer id, String code, String redirectUri, Date expireTime) {
        this.id = id;
        this.code = code;
        this.redirectUri = redirectUri;
        this.expireTime = expireTime;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getRedirectUri() {
        return redirectUri;
    }

    public void setRedirectUri(String redirectUri) {
        this.redirectUri = redirectUri;
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

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (id != null ? id.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof AuthzCode)) {
            return false;
        }
        AuthzCode other = (AuthzCode) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "org.consec.oauth2.authzserver.jpa.entities.AuthzCode[ id=" + id + " ]";
    }
    
}
