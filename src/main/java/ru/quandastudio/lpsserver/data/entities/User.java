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

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "users")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString
public class User implements Serializable {
	private static final long serialVersionUID = 1L;

	public enum State {
		@Deprecated
		unk, banned, ready, admin;
	}

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "user_id", precision = 6)
	private Integer userId;

	@Column(name = "name", length = 64)
	private String name;

	@Column(name = "auth_type")
	private String authType;

	@CreationTimestamp
	@Column(name = "reg_date")
	private Timestamp regDate;

	@UpdateTimestamp
	@Column(name = "last_visit")
	private Timestamp lastVisitDate;

	@Column(name = "sn_uid", length = 32)
	private String snUid;

	@Column(name = "acc_id", length = 8)
	private String accessId;

	@Enumerated(EnumType.STRING)
	@Column(name = "state")
	private State state;

	@Column(name = "firebase_token", length = 200)
	private String firebaseToken;

	@Override
	public int hashCode() {
		return Objects.hash(accessId, authType, firebaseToken, lastVisitDate, name, regDate, snUid, state, userId);
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
		return Objects.equals(accessId, other.accessId) && Objects.equals(authType, other.authType)
				&& Objects.equals(name, other.name) && Objects.equals(snUid, other.snUid)
				&& Objects.equals(userId, other.userId);
	}

}
