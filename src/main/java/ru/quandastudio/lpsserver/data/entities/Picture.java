package ru.quandastudio.lpsserver.data.entities;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
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

	public Picture(User owner, String image) {
		this(null, owner, image.getBytes());
	}
}
