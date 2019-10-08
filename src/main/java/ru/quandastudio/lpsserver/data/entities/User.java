package ru.quandastudio.lpsserver.data.entities;

import java.io.Serializable;
import java.sql.Timestamp;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "users")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class User implements Serializable {
	private static final long serialVersionUID = 1L;

	public enum State {
		unk, banned, ready, pva, pvp;
	}

	@Id
	@Column(name = "user_id", precision = 6)
	private Integer userId;

	@Column(name = "name", length = 64)
	private String name;

	@Column(name = "auth_type")
	private String authType;

	@Column(name = "reg_date")
	private Timestamp regDate;

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

}
