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

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "battle_history")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString
public class HistoryItem implements Serializable {
	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id", precision = 11)
	private Long id;

	@ManyToOne
	@JoinColumn(name = "starter_id")
	private User starter;

	@ManyToOne
	@JoinColumn(name = "invited_id")
	private User invited;

	@Column(name = "begin_time")
	private Timestamp creationDate;

	@Column(name = "duration", precision = 6)
	private Integer duration;

	@Column(name = "words_count", precision = 6)
	private Integer wordsCount;

	/**
	 * 
	 * @param sender     user who starts room
	 * @param invited    user's opponent
	 * @param startTime  time in millis when game started
	 * @param duration   duration of this battle in seconds
	 * @param wordsCount total count of words used in the battle
	 */
	public HistoryItem(User starter, User invited, long startTime, Integer duration, Integer wordsCount) {
		this(null, starter, invited, new Timestamp(startTime), duration, wordsCount);
	}
}
