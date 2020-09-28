package ru.quandastudio.lpsserver.data.entities;

import lombok.*;
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
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString
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
