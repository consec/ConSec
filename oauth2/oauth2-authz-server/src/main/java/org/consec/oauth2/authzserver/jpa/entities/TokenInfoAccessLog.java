package org.consec.oauth2.authzserver.jpa.entities;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;

@Entity
@Table(name = "token_info_access_log")
@NamedQueries({
        @NamedQuery(name = "TokenInfoAccessLog.findAll", query = "SELECT t FROM TokenInfoAccessLog t"),
        @NamedQuery(name = "TokenInfoAccessLog.findById", query = "SELECT t FROM TokenInfoAccessLog t WHERE t.id = :id"),
        @NamedQuery(name = "TokenInfoAccessLog.findByBearerName", query = "SELECT t FROM TokenInfoAccessLog t WHERE t.bearerName = :bearerName"),
        @NamedQuery(name = "TokenInfoAccessLog.findByResourceServerName", query = "SELECT t FROM TokenInfoAccessLog t WHERE t.resourceServerName = :resourceServerName")})
public class TokenInfoAccessLog implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(nullable = false)
    private Integer id;
    @Basic(optional = false)
    @Column(name = "bearer_name", nullable = false, length = 255)
    private String bearerName;
    @Basic(optional = false)
    @Column(name = "resource_server_name", nullable = false, length = 255)
    private String resourceServerName;
    @Basic(optional = false)
    @Column(nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date timestamp;
    @JoinColumn(name = "access_token_id", referencedColumnName = "id", nullable = false)
    @ManyToOne(optional = false)
    private AccessToken accessToken;

    public TokenInfoAccessLog() {
    }

    public TokenInfoAccessLog(Integer id) {
        this.id = id;
    }

    public TokenInfoAccessLog(Integer id, String bearerName, String resourceServerName, Date timestamp) {
        this.id = id;
        this.bearerName = bearerName;
        this.resourceServerName = resourceServerName;
        this.timestamp = timestamp;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getBearerName() {
        return bearerName;
    }

    public void setBearerName(String bearerName) {
        this.bearerName = bearerName;
    }

    public String getResourceServerName() {
        return resourceServerName;
    }

    public void setResourceServerName(String resourceServerName) {
        this.resourceServerName = resourceServerName;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public AccessToken getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(AccessToken accessToken) {
        this.accessToken = accessToken;
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
        if (!(object instanceof TokenInfoAccessLog)) {
            return false;
        }
        TokenInfoAccessLog other = (TokenInfoAccessLog) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "org.consec.oauth2.authzserver.jpa.entities.TokenInfoAccessLog[ id=" + id + " ]";
    }

    public JSONObject toJson() throws JSONException {
        JSONObject o = new JSONObject();
        o.put("id", id);
        o.put("access_token", accessToken.getToken());
        o.put("timestamp", timestamp);
        o.put("bearer", bearerName);
        o.put("resource_server", resourceServerName);
        return o;
    }
}
