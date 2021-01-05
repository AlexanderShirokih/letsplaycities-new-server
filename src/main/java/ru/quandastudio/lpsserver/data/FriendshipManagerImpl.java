package ru.quandastudio.lpsserver.data;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.quandastudio.lpsserver.data.dao.FriendshipRepository;
import ru.quandastudio.lpsserver.data.entities.Friendship;
import ru.quandastudio.lpsserver.data.entities.User;
import ru.quandastudio.lpsserver.models.FriendInfo;
import ru.quandastudio.lpsserver.models.FriendshipStatus;

import javax.transaction.Transactional;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class FriendshipManagerImpl implements FriendshipManager {

    private final FriendshipRepository friendshipDAO;

    @Override
    public void addNewRequest(Friendship friendship) {
        // Assert that we are not friends yet
        if (friendshipDAO.findBySenderAndReceiverOrReceiverAndSender(friendship.getSender(), friendship.getReceiver())
                .isEmpty())
            friendshipDAO.save(friendship);
    }

    @Override
    public void deleteFriend(User first, User second) {
        friendshipDAO.deleteBySenderAndReceiverOrReceiverAndSender(first, second);
    }

    @Override
    public Optional<Friendship> getFriendsInfo(User first, User second) {
        return friendshipDAO.findBySenderAndReceiverOrReceiverAndSender(first, second);
    }

    @Override
    public void markAcceptedIfExistsOrDelete(User sender, User receiver, boolean isAccepted) {
        final Optional<Friendship> friendInfo = friendshipDAO.findBySenderAndReceiverAndIsAcceptedFalse(sender, receiver);

        // Check if request exists or not
        friendInfo.ifPresent((Friendship info) -> {
            if (isAccepted && !info.getIsAccepted()) {
                info.setIsAccepted(true);
            } else {
                friendshipDAO.delete(info);
            }
        });
    }

    @Override
    public List<FriendInfo> getFriendsList(User user) {
        return friendshipDAO.findAllFriendsByUser(user)
                .stream()
                .map(FriendInfo::new)
                .collect(Collectors.toList());
    }

    @Override
    public void swapSenderAndReceiver(User newSender, User newReceiver) {
        friendshipDAO.swapSenderAndReceiver(newSender, newReceiver);
    }

    @Override
    public FriendshipStatus getFriendshipStatus(User user1, User user2) {
        return getFriendsInfo(user1, user2).map(friendship -> {
            if (friendship.getIsAccepted()) return FriendshipStatus.friends;
            if (friendship.getSender().getId().equals(user1.getId())) return FriendshipStatus.outputRequest;
            else return FriendshipStatus.inputRequest;
        }).orElse(FriendshipStatus.notFriends);
    }

}
