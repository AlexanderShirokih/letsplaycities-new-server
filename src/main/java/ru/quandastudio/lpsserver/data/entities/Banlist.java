package ru.quandastudio.lpsserver.data.entities;

import lombok.*;

import javax.persistence.*;
import java.io.Serializable;

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
@IdClass(BanlistIdKey.class)
public class Banlist implements Serializable {

	private static final long serialVersionUID = 3L;

	@Id
	@ManyToOne(cascade = CascadeType.ALL)
	@JoinColumn(name = "baner_id", referencedColumnName = "id")
	private User baner;

	@Id
	@ManyToOne(cascade = CascadeType.ALL)
	@JoinColumn(name = "banned_id", referencedColumnName = "id")
	private User banned;

}
