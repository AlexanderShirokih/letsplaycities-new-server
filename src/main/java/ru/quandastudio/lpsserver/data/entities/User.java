package ru.quandastudio.lpsserver.data.entities;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import ru.quandastudio.lpsserver.models.AuthType;
import ru.quandastudio.lpsserver.models.Role;

import javax.persistence.*;
import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Objects;

/**
 * Main entity representing User
 *
 * @author Alexander Shirokikh
 */
@Entity
@Table
public class User implements Serializable {
    private static final long serialVersionUID = 2L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", precision = 6)
    private Integer id;

    @Column(name = "name", length = 64, nullable = false)
    private String name;

    @UpdateTimestamp
    @Column(name = "last_visit")
    private Timestamp lastVisitDate;

    @Column(name = "avatar_hash", length = 32)
    private String avatarHash;

    @Enumerated(EnumType.ORDINAL)
    @Column(name = "role", nullable = false)
    private Role role;

    @Column(name = "sn_uid", length = 32)
    private String snUid;

    @Column(name = "access_hash", length = 8)
    private String accessHash;

    @Column(name = "firebase_token", length = 200)
    private String firebaseToken;

    @CreationTimestamp
    @Column(name = "reg_date")
    private Timestamp regDate;

    @Column(name = "auth_type", nullable = false)
    @Enumerated(EnumType.ORDINAL)
    private AuthType authType;

    public User(Integer id, String name, Timestamp lastVisitDate, String avatarHash, Role role, String snUid, String accessHash, String firebaseToken, Timestamp regDate, AuthType authType) {
        this.id = id;
        this.name = name;
        this.lastVisitDate = lastVisitDate;
        this.avatarHash = avatarHash;
        this.role = role;
        this.snUid = snUid;
        this.accessHash = accessHash;
        this.firebaseToken = firebaseToken;
        this.regDate = regDate;
        this.authType = authType;
    }

    public User() {
    }

    @Override
    public int hashCode() {
        return Objects.hash(lastVisitDate, name, id, role);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        User other = (User) obj;
        return Objects.equals(name, other.name)
                && Objects.equals(id, other.id) && Objects.equals(role, other.role);
    }

    public User(Integer id) {
        setId(id);
    }

    public Integer getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    public Timestamp getLastVisitDate() {
        return this.lastVisitDate;
    }

    public String getAvatarHash() {
        return this.avatarHash;
    }

    public Role getRole() {
        return this.role;
    }

    public String getSnUid() {
        return this.snUid;
    }

    public String getAccessHash() {
        return this.accessHash;
    }

    public String getFirebaseToken() {
        return this.firebaseToken;
    }

    public Timestamp getRegDate() {
        return this.regDate;
    }

    public AuthType getAuthType() {
        return this.authType;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setLastVisitDate(Timestamp lastVisitDate) {
        this.lastVisitDate = lastVisitDate;
    }

    public void setAvatarHash(String avatarHash) {
        this.avatarHash = avatarHash;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public void setSnUid(String snUid) {
        this.snUid = snUid;
    }

    public void setAccessHash(String accessHash) {
        this.accessHash = accessHash;
    }

    public void setFirebaseToken(String firebaseToken) {
        this.firebaseToken = firebaseToken;
    }

    public void setRegDate(Timestamp regDate) {
        this.regDate = regDate;
    }

    public void setAuthType(AuthType authType) {
        this.authType = authType;
    }

    public String toString() {
        return "User(id=" + this.getId() + ", name=" + this.getName() + ", lastVisitDate=" + this.getLastVisitDate() + ", avatarHash=" + this.getAvatarHash() + ", role=" + this.getRole() + ", snUid=" + this.getSnUid() + ", accessHash=" + this.getAccessHash() + ", firebaseToken=" + this.getFirebaseToken() + ", regDate=" + this.getRegDate() + ", authType=" + this.getAuthType() + ")";
    }
}
