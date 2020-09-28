package ru.quandastudio.lpsserver.models;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import ru.quandastudio.lpsserver.data.entities.HistoryProjection;

import java.sql.Timestamp;

@RequiredArgsConstructor
@Getter
@NonNull
@EqualsAndHashCode
public class HistoryInfo {
    final int userId;
    final String login;
    final boolean isFriend;
    final long startTime;
    final int duration;
    final int wordsCount;
    final String pictureHash;

    /**
     * Use {@link #startTime} instead.
     */
    @Deprecated(since = "1.4.3")
    final Timestamp creationDate;


    public HistoryInfo(HistoryProjection h) {
        this(h.getUserId(), h.getLogin(), h.getIsFriend(), h.getCreationDate().getTime(), h.getDuration(),
                h.getWordsCount(), h.getPictureHash(), h.getCreationDate());
    }
}
