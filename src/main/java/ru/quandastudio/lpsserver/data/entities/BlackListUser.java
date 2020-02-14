package ru.quandastudio.lpsserver.data.entities;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
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
public class BlackListUser implements Serializable {

	private static final long serialVersionUID = 2L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id", precision = 11)
	private Long id;

	@ManyToOne
	@JoinColumn(name = "baner_id")
	private User baner;

	@ManyToOne
	@JoinColumn(name = "banned_id")
	private User banned;

	public BlackListUser(User baner, User banned) {
		this(null, baner, banned);
	}

}
