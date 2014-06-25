
package org.consec.dynamicca.jpa.entities;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.io.Serializable;

@Embeddable
public class CertPK implements Serializable {
    @Basic(optional = false)
    @NotNull
    @Column(name = "sn", nullable = false)
    private int sn;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 36)
    @Column(name = "ca_uid", nullable = false, length = 36)
    private String caUid;

    public CertPK() {
    }

    public CertPK(int sn, String caUid) {
        this.sn = sn;
        this.caUid = caUid;
    }

    public int getSn() {
        return sn;
    }

    public void setSn(int sn) {
        this.sn = sn;
    }

    public String getCaUid() {
        return caUid;
    }

    public void setCaUid(String caUid) {
        this.caUid = caUid;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (int) sn;
        hash += (caUid != null ? caUid.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof CertPK)) {
            return false;
        }
        CertPK other = (CertPK) object;
        if (this.sn != other.sn) {
            return false;
        }
        if ((this.caUid == null && other.caUid != null) || (this.caUid != null && !this.caUid.equals(other.caUid))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "org.consec.dynamicca.jpa.entities.CertPK[ sn=" + sn + ", caUid=" + caUid + " ]";
    }

}
