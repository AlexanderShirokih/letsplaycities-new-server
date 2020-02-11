package ru.quandastudio.lpsserver.http.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import ru.quandastudio.lpsserver.core.ServerContext;
import ru.quandastudio.lpsserver.data.entities.BannedUser;
import ru.quandastudio.lpsserver.data.entities.User;
import ru.quandastudio.lpsserver.http.model.MessageWrapper;
import ru.quandastudio.lpsserver.models.BlackListItem;

@RestController
@RequiredArgsConstructor
public class BlackListController {

	@Autowired
	private ServerContext context;

	@GetMapping(value = "/user/{id}/blacklist")
	@ResponseBody
	public MessageWrapper<List<BlackListItem>> getBlackList(@PathVariable(name="id") Integer userId,
			@RequestParam(name = "hash") String accessHash) {
		return context.getUserManager()
				.getUserByIdAndHash(userId, accessHash)
				.map((User u) -> context.getBanlistManager().getBannedUsers(u))
				.map(BlackListController::convert)
				.wrap();
	}

	private static List<BlackListItem> convert(List<BannedUser> bannedList) {
		return bannedList.stream().map((BannedUser banned) -> new BlackListItem(banned)).collect(Collectors.toList());
	}

}
