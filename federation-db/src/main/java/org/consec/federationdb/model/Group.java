package org.consec.federationdb.model;

import javax.persistence.*;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.util.List;

@Entity
@Table(name = "\"group\"")
@NamedQueries({
        @NamedQuery(name = "Group.findAll", query = "SELECT g FROM Group g")})
public class Group implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "group_id", nullable = false)
    private Integer groupId;
    @Size(max = 45)
    @Column(name = "name", length = 45)
    private String name;
    @Size(max = 255)
    @Column(name = "description", length = 255)
    private String description;
    @JoinTable(name = "user_has_group", joinColumns = {
            @JoinColumn(name = "group_id", referencedColumnName = "group_id", nullable = false)}, inverseJoinColumns = {
            @JoinColumn(name = "user_id", referencedColumnName = "user_id", nullable = false)})
    @ManyToMany
    private List<User> userList;

    public Group() {
    }

    public Group(Integer groupId) {
        this.groupId = groupId;
    }

    public Integer getGroupId() {
        return groupId;
    }

    public void setGroupId(Integer groupId) {
        this.groupId = groupId;
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

    public List<User> getUserList() {
        return userList;
    }

    public void setUserList(List<User> userList) {
        this.userList = userList;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (groupId != null ? groupId.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof Group)) {
            return false;
        }
        Group other = (Group) object;
        if ((this.groupId == null && other.groupId != null) || (this.groupId != null && !this.groupId.equals(other.groupId))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "org.consec.federationdb.model.Group[ groupId=" + groupId + " ]";
    }

}
