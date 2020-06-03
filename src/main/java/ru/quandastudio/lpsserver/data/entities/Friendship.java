package ru.quandastudio.lpsserver.data.entities;

import java.io.Serializable;
import java.sql.Timestamp;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.annotations.CreationTimestamp;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table
@NoArgsConstructor
@AllArgsConstructor
@Data
@IdClass(CompositeFriendshipKey.class)
public class Friendship implements Serializable {
	private static final long serialVersionUID = 2L;

	@Id
	@ManyToOne
	@JoinColumn(name = "sender_id", referencedColumnName = "user_id")
	private User sender;
	
	@Id
	@ManyToOne
	@JoinColumn(name = "receiver_id", referencedColumnName = "user_id")
	private User receiver;

	@Column(name = "accepted")
	private Boolean isAccepted;

	@CreationTimestamp
	@Column(name = "creation_date")
	private Timestamp creationDate;

	public Friendship(User sender, User receiver) {
		this(sender, receiver, false, null);
	}

}
