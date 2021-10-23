package ru.quandastudio.lpsserver.core.game;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ru.quandastudio.lpsserver.core.*;
import ru.quandastudio.lpsserver.data.entities.History;
import ru.quandastudio.lpsserver.data.entities.Picture;
import ru.quandastudio.lpsserver.data.entities.User;
import ru.quandastudio.lpsserver.models.FriendshipStatus;
import ru.quandastudio.lpsserver.models.LPSMessage;
import ru.quandastudio.lpsserver.models.WordResult;

import java.util.ArrayList;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
public class Room {

    public static final int MOVE_TIME_MS = 92000;
    private static final int MOVE_TIMER_PERIOD = 5000;

    private final Player starter;
    private final Player invited;

    private ArrayList<String> usedWords = new ArrayList<>();

    private Dictionary dictionary;
    private Player current;
    private long startTime;
    private char lastChar;
    private int moveCounter = 0;

    public boolean start() {
        final ServerContext context = starter.getCurrentContext();
        dictionary = context.getDictionary();
        current = starter;

        final FriendshipStatus friendshipStatus = context.getFriendshipManager()
                .getFriendshipStatus(starter.getUser(), invited.getUser());

        boolean isBanned = context.getBanlistManager().isBanned(starter.getUser(), invited.getUser());

        sendPlayMessage(starter, true, friendshipStatus, isBanned, invited);
        sendPlayMessage(invited, false, friendshipStatus, isBanned, starter);

        if (isBanned)
            return false;

        startTimer();
        startTime = System.currentTimeMillis();

        return true;
    }

    // Игрок дал свой ответ
    public void word(Player player, String word) {
        if (player.equals(current)) {
            if (isValid(word)) {
                if (usedWords.contains(word)) {
                    submitWord(player, player, word, WordResult.ALREADY);
                } else if (dictionary.contains(word)) {
                    int end = word.length();
                    do {
                        if (end == 0) {
                            log.warn("ERROR!!! end of letters for word={}", word);
                            return;
                        }
                        end--;
                        lastChar = word.charAt(end);
                    } while (lastChar == 'ь' || lastChar == 'ъ' || lastChar == 'ы' || lastChar == 'ё');

                    usedWords.add(word);
                    submitWord(player, player, word, WordResult.ACCEPTED);
                    current = oppositePlayer(current);
                    submitWord(current, player, word, WordResult.RECEIVED);
                    startTimer();
                } else {
                    submitWord(player, player, word, WordResult.NO_WORD);
                }
            } else {
                submitWord(player, player, word, WordResult.NO_WORD);
            }
        } else {
            submitWord(player, player, word, WordResult.WRONG_MOVE);
        }
    }

    private void submitWord(Player player, Player owner, String word, WordResult result) {
        player.sendMessage(new LPSMessage.LPSWordMessage(result, word, owner.getUser().getId()));
    }

    @SuppressWarnings("deprecation")
    private void sendPlayMessage(Player player, boolean isStarter, FriendshipStatus friendshipStatus, boolean isBanned, Player other) {
        final User oppUser = other.getUser();
        final String login = oppUser.getName();
        final String avatar = getAvatarForOlderVersions(player, other.getUser());
        boolean isFriend = friendshipStatus == FriendshipStatus.friends;
        final LPSMessage.LPSPlayMessage play = new LPSMessage.LPSPlayMessage(other.getUser().getAuthType(), login, oppUser.getId(),
                other.getClientVersion(), other.getClientBuild(), other.getCanReceiveMessages(), isFriend, friendshipStatus, isStarter,
                isBanned, oppUser.getAvatarHash(), false);

        if (avatar != null && !avatar.isEmpty()) {
            play.setAvatar(avatar);
        }

        player.sendMessage(play);
    }

    private String getAvatarForOlderVersions(Player player, User user) {
        return Optional.of(player)
                .filter((Player) -> !player.isAtLeastHasVersion(270))
                .flatMap((Player p) -> p.getCurrentContext().getPictureManager().getPictureByUserId(user))
                .map((Picture picture) -> new String(picture.getImageData()))
                .orElse(null);
    }

    private void startTimer() {
        moveCounter++;
        GameTimer gameTimer = new GameTimer(this, moveCounter);
        current.getCurrentContext()
                .getTaskLooper()
                .schedule(new DelayedTask(MOVE_TIMER_PERIOD, MOVE_TIMER_PERIOD, gameTimer));
    }

    private boolean isValid(String str) {
        if (str.length() < 2 || (lastChar != 0 && str.charAt(0) != lastChar)) {
            log.warn("ii={} ;lc={}", str, lastChar);
            return false;
        }
        return true;
    }

    public boolean isUsed(String word) {
        return usedWords.contains(word);
    }

    public Player oppositePlayer(Player player) {
        return player.equals(starter) ? invited : starter;
    }

    /**
     * Битва завершена. Подводим итоги.
     */
    public void finish() {
        if (startTime > 0 && moveCounter > 1) {
            ServerContext context = starter.getCurrentContext();
            context.getHistoryManager().addHistoryItem(makeHistoryItem());
        }

        if (usedWords != null) {
            usedWords.clear();
            usedWords = null;
        }
        moveCounter = 0;
        startTime = 0;
    }

    private History makeHistoryItem() {
        int duration = (int) ((System.currentTimeMillis() - startTime) / 1000L);
        return new History(starter.getUser(), invited.getUser(), startTime, duration, usedWords.size());
    }

    private void timeOut() {
        finish();

        timeOutPlayer(starter);
        timeOutPlayer(invited);
    }

    private void timeOutPlayer(Player player) {
        if (player.isOnline())
            player.sendMessage(new LPSMessage.LPSTimeoutMessage());
        player.setOnline(false);
        player.setRoom(null);
    }

    class GameTimer implements RunnableTask {
        private final Room r;
        private final int mID;
        @Getter
        private int time;

        public GameTimer(Room r, int mID) {
            this.r = r;
            this.mID = mID;
            time = 0;
        }

        @Override
        public void run(DelayedTask task) {
            time += MOVE_TIMER_PERIOD;
            if (time >= MOVE_TIME_MS) {
                if (mID == r.moveCounter)
                    r.timeOut();
                task.cancel();
            } else {
                if (mID == r.moveCounter) {
                    if (!starter.isOnline() || !invited.isOnline())
                        task.cancel();
                } else
                    task.cancel();
            }
        }
    }

}
