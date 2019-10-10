package ru.quandastudio.lpsserver.data.entities;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "banlist")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString
public class BannedUser implements Serializable {

	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id", precision = 11)
	private Long id;

	@Column(name = "baner_id", precision = 6)
	private Integer banerId;

	@Column(name = "banned_id", precision = 6)
	private Integer bannedId;

	@Column(name = "banned_username", length = 64)
	private String bannedName;

	public BannedUser(Integer banerId, Integer bannedId, String bannedName) {
		this(null, banerId, bannedId, bannedName);
	}

}
