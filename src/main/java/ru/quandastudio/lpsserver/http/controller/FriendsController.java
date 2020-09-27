package ru.quandastudio.lpsserver.http.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import ru.quandastudio.lpsserver.core.ServerContext;
import ru.quandastudio.lpsserver.data.FriendshipManager;
import ru.quandastudio.lpsserver.data.entities.Friendship;
import ru.quandastudio.lpsserver.data.entities.FriendshipProjection;
import ru.quandastudio.lpsserver.data.entities.User;
import ru.quandastudio.lpsserver.models.RequestType;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/friend")
public class FriendsController {

    private final ServerContext context;

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public void deleteFriend(@PathVariable("id") int friendId, @AuthenticationPrincipal User user) {
        context.getFriendshipManager().deleteFriend(user, new User(friendId));
    }

    @PutMapping("/request/{id}/{type}")
    public ResponseEntity<String> handleRequest(@PathVariable("id") int userId,
                                                @PathVariable("type") RequestType requestType, @AuthenticationPrincipal User user) {
        final FriendshipManager friendshipManager = context.getFriendshipManager();
        final User oppUser = new User(userId);

        switch (requestType) {
            case ACCEPT:
                friendshipManager.markAcceptedIfExistsOrDelete(oppUser, user, true);
                break;
            case DENY:
                friendshipManager.markAcceptedIfExistsOrDelete(oppUser, user, false);
                break;
            default:
                friendshipManager.addNewRequest(new Friendship(user, oppUser));
                break;
        }
        return ResponseEntity.ok("ok");
    }

    @GetMapping("/")
    @ResponseBody
    public List<FriendshipProjection> getFriendInfo(@AuthenticationPrincipal User user) {
        return context.getFriendshipManager().getFriendsList(user);
    }

}
