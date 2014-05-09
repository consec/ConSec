package org.consec.federationdb.model;

import javax.persistence.*;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.util.List;

@Entity
@Table(name = "role")
@NamedQueries({
        @NamedQuery(name = "Role.findAll", query = "SELECT r FROM Role r")})
public class Role implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "role_id", nullable = false)
    private Integer roleId;
    @Size(max = 45)
    @Column(name = "name", length = 45)
    private String name;
    @Size(max = 255)
    @Column(name = "description", length = 255)
    private String description;
    @Lob
    @Size(max = 65535)
    @Column(name = "acl", length = 65535)
    private String acl;
    @JoinTable(name = "user_has_role", joinColumns = {
            @JoinColumn(name = "role_id", referencedColumnName = "role_id", nullable = false)}, inverseJoinColumns = {
            @JoinColumn(name = "user_id", referencedColumnName = "user_id", nullable = false)})
    @ManyToMany
    private List<User> userList;

    public Role() {
    }

    public Role(Integer roleId) {
        this.roleId = roleId;
    }

    public Integer getRoleId() {
        return roleId;
    }

    public void setRoleId(Integer roleId) {
        this.roleId = roleId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getAcl() {
        return acl;
    }

    public void setAcl(String acl) {
        this.acl = acl;
    }

    public List<User> getUserList() {
        return userList;
    }

    public void setUserList(List<User> userList) {
        this.userList = userList;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (roleId != null ? roleId.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof Role)) {
            return false;
        }
        Role other = (Role) object;
        if ((this.roleId == null && other.roleId != null) || (this.roleId != null && !this.roleId.equals(other.roleId))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "org.consec.federationdb.model.Role[ roleId=" + roleId + " ]";
    }

}
