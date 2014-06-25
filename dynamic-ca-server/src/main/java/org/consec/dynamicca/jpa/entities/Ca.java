package org.consec.dynamicca.jpa.entities;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.util.List;

@Entity
@Table(name = "ca")
@NamedQueries({
        @NamedQuery(name = "Ca.findAll", query = "SELECT c FROM Ca c"),
        @NamedQuery(name = "Ca.findByUid", query = "SELECT c FROM Ca c WHERE c.uid = :uid"),
        @NamedQuery(name = "Ca.findBySeqNum", query = "SELECT c FROM Ca c WHERE c.seqNum = :seqNum")})
public class Ca implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 36)
    @Column(name = "uid", nullable = false, length = 36)
    private String uid;
    @Basic(optional = false)
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "seq_num", nullable = false)
    private int seqNum;
    @Size(max = 100)
    @Column(name = "name", length = 100)
    private String name;
    @Lob
    @Size(max = 65535)
    @Column(name = "private_key", length = 65535)
    private String privateKey;
    @Lob
    @Size(max = 65535)
    @Column(name = "certificate", length = 65535)
    private String certificate;
    @Column(name = "cert_sn_counter")
    private int certSnCounter = 1;
    @Column(name = "crl_counter")
    private int crlCounter = 1;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "ca")
    private List<Cert> certList;

    public Ca() {
    }

    public Ca(String uid) {
        this.uid = uid;
    }

    public Ca(String uid, int seqNum) {
        this.uid = uid;
        this.seqNum = seqNum;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public int getSeqNum() {
        return seqNum;
    }

    public void setSeqNum(int seqNum) {
        this.seqNum = seqNum;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public int getCertSnCounter() {
        return certSnCounter;
    }

    public void setCertSnCounter(int certSnCounter) {
        this.certSnCounter = certSnCounter;
    }

    public int getCrlCounter() {
        return crlCounter;
    }

    public void setCrlCounter(int crlCounter) {
        this.crlCounter = crlCounter;
    }

    public List<Cert> getCertList() {
        return certList;
    }

    public void setCertList(List<Cert> certList) {
        this.certList = certList;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (uid != null ? uid.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof Ca)) {
            return false;
        }
        Ca other = (Ca) object;
        if ((this.uid == null && other.uid != null) || (this.uid != null && !this.uid.equals(other.uid))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "org.consec.dynamicca.jpa.entities.Ca[ uid=" + uid + " ]";
    }
}
