package ru.quandastudio.lpsserver.data.entities;

import java.io.Serializable;
import java.sql.Timestamp;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.MapsId;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import org.hibernate.annotations.CreationTimestamp;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Entity that contains authorization data for each user
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
public class AuthData implements Serializable {

	private static final long serialVersionUID = 1L;
	
	@Id
	private Integer id;
	
	@MapsId
	@OneToOne(optional = false, cascade = CascadeType.ALL, orphanRemoval = true)
	@JoinColumn(name = "user_id")
	private User user;

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
	private String authType;

}
