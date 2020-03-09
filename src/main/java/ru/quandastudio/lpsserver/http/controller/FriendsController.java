package ru.quandastudio.lpsserver.http.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import ru.quandastudio.lpsserver.core.ServerContext;
import ru.quandastudio.lpsserver.data.FriendshipManager;
import ru.quandastudio.lpsserver.data.entities.FriendshipProjection;
import ru.quandastudio.lpsserver.data.entities.User;
import ru.quandastudio.lpsserver.models.RequestType;

@RestController
@RequiredArgsConstructor
@RequestMapping("/friend")
public class FriendsController {

	@Autowired
	private ServerContext context;

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.OK)
	public void getBlackList(@PathVariable("id") int friendId, @AuthenticationPrincipal User user) {
		context.getFriendshipManager().deleteFriend(user, new User(friendId));
	}

	@PutMapping("/request/{id}/{type}")
	public ResponseEntity<String> handleRequest(@PathVariable("id") int userId,
			@PathVariable("type") RequestType requestType, @AuthenticationPrincipal User user) {
		final FriendshipManager friendshipManager = context.getFriendshipManager();
		final User oppUser = new User(userId);

		switch (requestType) {
		case ACCEPT:
			friendshipManager.markAcceptedIfExistsOrDelete(user, oppUser, true);
			break;
		case DENY:
			friendshipManager.markAcceptedIfExistsOrDelete(user, oppUser, true);
			break;
		default:
			return ResponseEntity.badRequest().build();
		}
		return ResponseEntity.ok("ok");
	}

	@GetMapping("/")
	@ResponseBody
	public List<FriendshipProjection> getFriendInfo(@AuthenticationPrincipal User user) {
		return context.getFriendshipManager().getFriendsList(user);
	}

}
