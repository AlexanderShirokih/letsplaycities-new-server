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

import org.hibernate.annotations.UpdateTimestamp;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

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

	public enum State {
		banned, ready, admin;

		/**
		 * Checks that entity is at least in {@code state}
		 * 
		 * @param state required state
		 * @return {@literal true} if entity is at least in {@code state},
		 *         {@literal false} otherwise
		 */
		public boolean isAtLeast(State state) {
			return ordinal() >= state.ordinal();
		}
	}

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "user_id", precision = 6)
	private Integer userId;

	@Column(name = "name", length = 64, nullable = false)
	private String name;

	@UpdateTimestamp
	@Column(name = "last_visit")
	private Timestamp lastVisitDate;

	@Column(name = "avatar_hash", length = 32)
	private String avatarHash;

	@Enumerated(EnumType.STRING)
	@Column(name = "state", nullable = false)
	private State state;

	@Override
	public int hashCode() {
		return Objects.hash(lastVisitDate, name, userId, state);
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
				&& Objects.equals(userId, other.userId) && Objects.equals(state, other.state);
	}

	public User(Integer userId) {
		setUserId(userId);
	}

}
