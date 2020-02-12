package ru.quandastudio.lpsserver.http.controller;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import ru.quandastudio.lpsserver.Result;
import ru.quandastudio.lpsserver.core.ServerContext;
import ru.quandastudio.lpsserver.data.entities.BannedUser;
import ru.quandastudio.lpsserver.data.entities.Friendship;
import ru.quandastudio.lpsserver.data.entities.HistoryItem;
import ru.quandastudio.lpsserver.data.entities.Picture;
import ru.quandastudio.lpsserver.data.entities.User;
import ru.quandastudio.lpsserver.http.model.MessageWrapper;
import ru.quandastudio.lpsserver.models.BlackListItem;
import ru.quandastudio.lpsserver.models.FriendInfo;
import ru.quandastudio.lpsserver.models.HistoryInfo;

@RestController
@RequiredArgsConstructor
@RequestMapping("/user")
public class UserController {

	@Autowired
	private ServerContext context;

	@RequestMapping(value = "/{id}/picture", produces = MediaType.TEXT_PLAIN_VALUE)
	public ResponseEntity<ByteArrayResource> getPicture(@PathVariable(value = "id") String userId) {
		return Result.of(() -> new User(Integer.parseInt(userId)))
				.map((User user) -> context.getPictureManager().getPictureByUserId(user))
				.flatMap((Optional<Picture> picture) -> Result.from(picture, "No picture"))
				.map((Picture p) -> new ByteArrayResource(p.getImageData()))
				.map((ByteArrayResource resource) -> ResponseEntity.ok()
						.contentType(MediaType.parseMediaType(MediaType.APPLICATION_OCTET_STREAM_VALUE))
						.contentLength(resource.contentLength())
						.header("Base64-Encoded", "true")
						.body(resource))
				.getOr(ResponseEntity.notFound().build());
	}

	@GetMapping("/blacklist")
	@ResponseBody
	public MessageWrapper<List<BlackListItem>> getBlackList(@AuthenticationPrincipal User user) {
		return Result.success(context.getBanlistManager().getBannedUsers(user)).map(UserController::convert).wrap();
	}

	private static List<BlackListItem> convert(List<BannedUser> bannedList) {
		return bannedList.stream().map((BannedUser banned) -> new BlackListItem(banned)).collect(Collectors.toList());
	}

	@GetMapping("/friends")
	@ResponseBody
	public MessageWrapper<List<FriendInfo>> getFriendInfo(@AuthenticationPrincipal User user) {
		return Result.success(context.getFriendshipManager().getFriendsList(user))
				.map((List<Friendship> fs) -> convertFriendsList(user.getUserId(), fs))
				.wrap();
	}

	private static List<FriendInfo> convertFriendsList(Integer userId, List<Friendship> list) {
		return list.stream().map((Friendship fs) -> transform(userId, fs)).collect(Collectors.toList());
	}

	private static FriendInfo transform(Integer playerId, Friendship fs) {
		final User oppUser = fs.getReceiver().getUserId() == playerId ? fs.getSender() : fs.getReceiver();
		return new FriendInfo(oppUser.getUserId(), oppUser.getName(), fs.getIsAccepted(), oppUser.getAvatarHash());
	}

	@GetMapping("/history")
	@ResponseBody
	public MessageWrapper<List<HistoryInfo>> getHistory(@AuthenticationPrincipal User user) {
		return Result.success(context.getHistoryManager().getHistoryList(user))
				.map((List<HistoryItem> list) -> convert(user.getUserId(), list))
				.wrap();
	}

	private List<HistoryInfo> convert(Integer userId, List<HistoryItem> userHistory) {
		final List<HistoryInfo> historyInfo = userHistory.stream()
				.map((HistoryItem banned) -> transform(userId, banned))
				.collect(Collectors.toList());
		return historyInfo;
	}

	private static HistoryInfo transform(Integer userId, HistoryItem item) {
		final User invited = item.getInvited();
		final User starter = item.getStarter();
		final User other = (invited.getUserId() == userId) ? starter : invited;

		return new HistoryInfo(other, item);
	}
}
