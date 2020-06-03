package ru.quandastudio.lpsserver.data.entities;

import java.io.Serializable;
import java.util.Objects;

import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.MapsId;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Used to store user avatar to database
 * 
 * @author Alexander Shirokikh
 *
 */
@Entity
@Table
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
public class Picture implements Serializable {

	private static final long serialVersionUID = 2L;

	public enum Type {
		BASE64(null), PNG("image/png"), JPEG("image/jpeg"), GIF("image/gif");

		private String name;

		Type(String name) {
			this.name = name;
		}

		public String getMediaType() {
			return name;
		}
	}

	@Id
	private Integer id;

	@MapsId
	@OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
	@JoinColumn(name = "owner_id")
	private User owner;

	@Lob
	@Basic(fetch = FetchType.LAZY)
	@Column(name = "image", nullable = false)
	private byte[] imageData;

	// b64 or raw
	@Column(name = "type", nullable = false)
	@Enumerated(EnumType.ORDINAL)
	private Type type;

	@Override
	public int hashCode() {
		return owner.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Picture other = (Picture) obj;
		return Objects.equals(owner, other.owner);
	}

}
