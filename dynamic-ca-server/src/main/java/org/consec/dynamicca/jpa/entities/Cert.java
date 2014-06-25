package org.consec.dynamicca.jpa.entities;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.util.Date;

@Entity
@Table(name = "cert")
@NamedQueries({
        @NamedQuery(name = "Cert.findAll", query = "SELECT c FROM Cert c"),
        @NamedQuery(name = "Cert.find",
                query = "SELECT c FROM Cert c WHERE c.certPK.caUid = :caUid AND c.certPK.sn = :sn"),
        @NamedQuery(name = "Cert.getRevokedCerts",
                query = "SELECT c FROM Cert c WHERE c.certPK.caUid = :caUid AND c.revoked = true")})
public class Cert implements Serializable {
    private static final long serialVersionUID = 1L;
    @EmbeddedId
    protected CertPK certPK;
    @Lob
    @Size(max = 65535)
    @Column(name = "private_key", length = 65535)
    private String privateKey;
    @Lob
    @Size(max = 65535)
    @Column(name = "certificate", length = 65535)
    private String certificate;
    @Basic(optional = false)
    @NotNull
    @Column(name = "revoked", nullable = false)
    private boolean revoked;
    @Column(name = "revocation_date")
    @Temporal(TemporalType.TIMESTAMP)
    private Date revocationDate;
    @JoinColumn(name = "ca_uid", referencedColumnName = "uid", nullable = false, insertable = false, updatable = false)
    @ManyToOne(optional = false)
    private Ca ca;

    public Cert() {
    }

    public Cert(CertPK certPK) {
        this.certPK = certPK;
    }

    public Cert(int sn, String caUid) {
        this.certPK = new CertPK(sn, caUid);
    }

    public CertPK getCertPK() {
        return certPK;
    }

    public void setCertPK(CertPK certPK) {
        this.certPK = certPK;
    }

    public String getPrivateKey() {
        return privateKey;
    }

    public void setPrivateKey(String privateKey) {
        this.privateKey = privateKey;
    }

    public String getCertificate() {
        return certificate;
    }

    public void setCertificate(String certificate) {
        this.certificate = certificate;
    }

    public boolean getRevoked() {
        return revoked;
    }

    public void setRevoked(boolean revoked) {
        this.revoked = revoked;
    }

    public Date getRevocationDate() {
        return revocationDate;
    }

    public void setRevocationDate(Date revocationDate) {
        this.revocationDate = revocationDate;
    }

    public Ca getCa() {
        return ca;
    }

    public void setCa(Ca ca) {
        this.ca = ca;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (certPK != null ? certPK.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof Cert)) {
            return false;
        }
        Cert other = (Cert) object;
        if ((this.certPK == null && other.certPK != null) || (this.certPK != null && !this.certPK.equals(other.certPK))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "org.consec.dynamicca.jpa.entities.Cert[ certPK=" + certPK + " ]";
    }

}
