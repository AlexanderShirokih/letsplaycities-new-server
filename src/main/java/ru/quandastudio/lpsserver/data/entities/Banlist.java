package ru.quandastudio.lpsserver.data.entities;

import java.io.Serializable;
import java.util.Objects;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Represents blacklist where user {@code baner} block {@code banned}
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
@IdClass(Banlist.class)
public class Banlist implements Serializable {

	private static final long serialVersionUID = 3L;

	@Id
	@ManyToOne(cascade = CascadeType.ALL)
	@JoinColumn(name = "baner_id", referencedColumnName = "user_id")
	private User baner;

	@Id
	@ManyToOne(cascade = CascadeType.ALL)
	@JoinColumn(name = "banned_id", referencedColumnName = "user_id")
	private User banned;

	@Override
	public int hashCode() {
		return Objects.hash(baner.getUserId(), banned.getUserId());
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Banlist other = (Banlist) obj;
		return Objects.equals(baner.getUserId(), other.baner.getUserId()) && Objects.equals(banned.getUserId(), other.banned.getUserId());
	}

	
}
