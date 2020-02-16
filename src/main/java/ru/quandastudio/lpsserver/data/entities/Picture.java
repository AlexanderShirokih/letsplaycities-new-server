package ru.quandastudio.lpsserver.data.entities;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "pictures")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
public class Picture {

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
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id", precision = 11)
	private Long id;

	@Basic(fetch = FetchType.LAZY)
	@OneToOne
	@JoinColumn(name = "owner_id", nullable = false, unique = true)
	private User owner;

	@Lob
	@Basic(fetch = FetchType.LAZY)
	@Column(name = "image", nullable = false)
	private byte[] imageData;

	// b64 or raw
	@Column(name = "type", nullable = false)
	@Enumerated(EnumType.ORDINAL)
	private Type type;

	public Picture(User owner, byte[] image, Type format) {
		this(null, owner, image, format);
	}
}
