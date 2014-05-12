package org.consec.oauth2.authzserver.jpa.entities;

import org.consec.oauth2.authzserver.jpa.enums.AuthorizedGrantType;

import javax.persistence.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "client")
@NamedQueries({
        @NamedQuery(name = "Client.findAll", query = "SELECT c FROM Client c"),
        @NamedQuery(name = "Client.findById", query = "SELECT c FROM Client c WHERE c.id = :id"),
        @NamedQuery(name = "Client.findByClientId", query = "SELECT c FROM Client c WHERE c.clientId = :clientId")})
public class Client implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(nullable = false)
    private Integer id;
    @Basic(optional = false)
    @Column(name = "client_id", nullable = false, length = 100)
    private String clientId;
    @Basic(optional = false)
    @Column(nullable = false, length = 100)
    private String name;
    @Basic(optional = true)
    @Column(name = "callback_uri", nullable = true, length = 256)
    private String callbackUri;
    @Basic(optional = false)
    @Column(name = "client_secret", nullable = false, length = 256)
    private String clientSecret;
    @Basic(optional = false)
    @Column(name = "authorized_grant_types", nullable = false, length = 37)
    private String authorizedGrantTypes;
    @JoinTable(name = "client_has_country", joinColumns = {
            @JoinColumn(name = "client_id", referencedColumnName = "id", nullable = false)}, inverseJoinColumns = {
            @JoinColumn(name = "country_code", referencedColumnName = "code", nullable = false)})
    @ManyToMany(targetEntity = Country.class)
    private List<Country> countryList;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "client")
    private List<ClientTrust> clientTrustList;
    @JoinColumn(name = "organization_id", referencedColumnName = "id", nullable = false)
    @ManyToOne(optional = false)
    private Organization organization;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "client")
    private List<AuthzCode> authzCodeList;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "client")
    private List<AccessToken> accessTokenList;

    public Client() {
    }

    public Client(Integer id) {
        this.id = id;
    }

    public Client(Integer id, String clientId, String name, String callbackUri, String clientSecret, String authorizedGrantTypes) {
        this.id = id;
        this.clientId = clientId;
        this.name = name;
        this.callbackUri = callbackUri;
        this.clientSecret = clientSecret;
        this.authorizedGrantTypes = authorizedGrantTypes;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCallbackUri() {
        return callbackUri;
    }

    public void setCallbackUri(String callbackUri) {
        this.callbackUri = callbackUri;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public List<AuthorizedGrantType> getAuthorizedGrantTypes() {
        List<AuthorizedGrantType> agtList = new ArrayList<AuthorizedGrantType>();
        for (String agtString : authorizedGrantTypes.split(",")) {
            AuthorizedGrantType agt = AuthorizedGrantType.valueOf(agtString);
            agtList.add(agt);
        }
        return agtList;
    }

    public void setAuthorizedGrantTypes(List<AuthorizedGrantType> authorizedGrantTypesList) {
        authorizedGrantTypes = "";
        for (int i = 0; i < authorizedGrantTypesList.size(); i++) {
            if (i > 0) {
                authorizedGrantTypes += ",";
            }
            authorizedGrantTypes += authorizedGrantTypesList.get(i).name();
        }
    }

    public List<ClientTrust> getClientTrustList() {
        return clientTrustList;
    }

    public void setClientTrustList(List<ClientTrust> clientTrustList) {
        this.clientTrustList = clientTrustList;
    }

    public Organization getOrganization() {
        return organization;
    }

    public void setOrganization(Organization organization) {
        this.organization = organization;
    }

    public List<AuthzCode> getAuthzCodeList() {
        return authzCodeList;
    }

    public void setAuthzCodeList(List<AuthzCode> authzCodeList) {
        this.authzCodeList = authzCodeList;
    }

    public List<AccessToken> getAccessTokenList() {
        return accessTokenList;
    }

    public void setAccessTokenList(List<AccessToken> accessTokenList) {
        this.accessTokenList = accessTokenList;
    }

    public List<Country> getCountryList() {
        return countryList;
    }

    public void setCountryList(List<Country> countryList) {
        this.countryList = countryList;
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
        if (!(object instanceof Client)) {
            return false;
        }
        Client other = (Client) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "org.consec.oauth2.authzserver.jpa.entities.Client[ id=" + id + " ]";
    }

}
