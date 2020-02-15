package ru.quandastudio.lpsserver.http.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import ru.quandastudio.lpsserver.core.ServerContext;
import ru.quandastudio.lpsserver.data.entities.OppUserNameProjection;
import ru.quandastudio.lpsserver.data.entities.User;

@RestController
@RequiredArgsConstructor
@RequestMapping("/blacklist")
public class BlacklistController {
	@Autowired
	private ServerContext context;

	@DeleteMapping("/{id}")
	public void getBlackList(@PathVariable("id") int userId, @AuthenticationPrincipal User user) {
		context.getBanlistManager().removeFromBanlist(user, new User(userId));
	}

	@GetMapping("/")
	@ResponseBody
	public List<OppUserNameProjection> getBlackList(@AuthenticationPrincipal User user) {
		return context.getBanlistManager().getBannedUsers(user);
	}

}
