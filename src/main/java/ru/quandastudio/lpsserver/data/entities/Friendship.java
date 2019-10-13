package ru.quandastudio.lpsserver.data.entities;

import java.io.Serializable;
import java.sql.Timestamp;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.annotations.CreationTimestamp;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "friends")
@NoArgsConstructor
@AllArgsConstructor
@Data
public class Friendship implements Serializable {
	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id", precision = 11)
	private Long id;

	@ManyToOne
	@JoinColumn(name = "sender_id")
	private User sender;

	@ManyToOne
	@JoinColumn(name = "receiver_id")
	private User receiver;

	@Column(name = "accepted")
	private Boolean isAccepted;

	@CreationTimestamp
	@Column(name = "creation_date")
	private Timestamp creationDate;

	public Friendship(User sender, User receiver) {
		this(null, sender, receiver, false, null);
	}

}
