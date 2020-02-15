package ru.quandastudio.lpsserver.http.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import ru.quandastudio.lpsserver.core.ServerContext;
import ru.quandastudio.lpsserver.data.entities.HistoryProjection;
import ru.quandastudio.lpsserver.data.entities.User;

@RestController
@RequiredArgsConstructor
@RequestMapping("/history")
public class HistoryController {

	@Autowired
	private ServerContext context;

	@GetMapping("/")
	@ResponseBody
	public List<HistoryProjection> getHistory(@AuthenticationPrincipal User user) {
		return context.getHistoryManager().getHistoryList(user);
	}
}
