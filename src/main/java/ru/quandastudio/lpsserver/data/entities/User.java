package ru.quandastudio.lpsserver.data.entities;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import ru.quandastudio.lpsserver.models.AuthType;

/**
 * Main entity representing User
 *
 * @author Alexander Shirokikh
 */
@Entity
@Table
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString
public class User implements Serializable {
    private static final long serialVersionUID = 2L;

    @RequiredArgsConstructor
    @Getter
    public enum Role {
        BANNED_USER("banned"), REGULAR_USER("ready"), ADMIN("admin");

        final String legacyName;

        /**
         * Checks that entity is at least in {@code state}
         *
         * @param role required state
         * @return {@literal true} if entity is at least in {@code state},
         * {@literal false} otherwise
         */
        public boolean isAtLeast(Role role) {
            return ordinal() >= role.ordinal();
        }
    }

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

}
