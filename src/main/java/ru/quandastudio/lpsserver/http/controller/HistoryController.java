package ru.quandastudio.lpsserver.http.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import ru.quandastudio.lpsserver.core.ServerContext;
import ru.quandastudio.lpsserver.data.entities.User;
import ru.quandastudio.lpsserver.models.HistoryInfo;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/history")
public class HistoryController {

    private final ServerContext context;

    @GetMapping("/")
    @ResponseBody
    public List<HistoryInfo> getHistory(@AuthenticationPrincipal User user) {
        return context.getHistoryManager().getHistoryList(user);
    }

    @GetMapping("/{id}")
    @ResponseBody
    public List<HistoryInfo> getHistoryWithUser(@AuthenticationPrincipal User user, @PathVariable("id") int oppId) {
        return context.getHistoryManager().getHistoryListWithFriend(user, new User(oppId));
    }
}
