package ru.quandastudio.lpsserver.http.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import ru.quandastudio.lpsserver.core.ServerContext;
import ru.quandastudio.lpsserver.data.entities.OppUserNameProjection;
import ru.quandastudio.lpsserver.data.entities.User;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/blacklist")
public class BlacklistController {
	private final ServerContext context;

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
