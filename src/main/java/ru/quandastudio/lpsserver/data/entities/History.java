package ru.quandastudio.lpsserver.data.entities;

import java.io.Serializable;
import java.sql.Timestamp;

import javax.persistence.*;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Represents game history
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
public class History implements Serializable {
    private static final long serialVersionUID = 2L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
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
     * @param starter    user who starts room
     * @param invited    user's opponent
     * @param startTime  time in millis when game started
     * @param duration   duration of this battle in seconds
     * @param wordsCount total count of words used in the battle
     */
    public History(User starter, User invited, long startTime, Integer duration, Integer wordsCount) {
        this(null, starter, invited, new Timestamp(startTime), duration, wordsCount);
    }
}
